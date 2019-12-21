package actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import clustering.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import model.*;
import play.libs.Json;
import util.*;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Agent extends AbstractActor {

    public static Props props(ActorRef out, Config config) {
        return Props.create(Agent.class, out, config);
    }

    // states of this agent
    private ActorRef out;
    private Config config;
    private String keyword;
    private PostgreSQL postgreSQL;
    private List<PointTuple> pointTuples;
    /**
     * maps from ordering of points to tid
     * key - clusterKey
     * value - map array with the same order as Double[][] points,
     *         value[i] is the tid of point at ith position
     */
    private Map<String, Long[]> orderMaps;
    /**
     * map of SuperCluster instances
     * key - clusterKey
     * value - handle to SuperCluster instance
     */
    private Map<String, SuperCluster> superClusters;
    /**
     * map of SuperCluster visiting counters
     * key - clusterKey
     * value - counter, number of visiting times of the cluster
     */
    private Map<String, Integer> superClustersHits;
    /**
     * Maximum number of SuperCluster instances being kept in memory
     */
    private final int MAX_CLUSTERS = 30;
    private static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private Date start;
    private Date end;
    private int intervalDays;
    private int minZoom;
    private int maxZoom;

    @Inject
    public Agent(ActorRef out, Config config) {
        this.out = out;
        this.config = config;
        this.superClusters = new HashMap<>();
        this.superClustersHits = new HashMap<>();
        this.orderMaps = new HashMap<>();
        try {
            this.start = sdf.parse(this.config.getString("progressive.start"));
            this.end = sdf.parse(this.config.getString("progressive.end"));
        } catch(ParseException e) {
            e.printStackTrace();
        }
        this.intervalDays = this.config.getInt("progressive.interval");
        this.minZoom = this.config.getInt("cluster.min_zoom");
        this.maxZoom = this.config.getInt("cluster.max_zoom");

        // initialize constants
        Constants.MIN_LONGITUDE = this.config.getDouble("data.minLng");
        Constants.MIN_LATITUDE = this.config.getDouble("data.minLat");
        Constants.MAX_LONGITUDE = this.config.getDouble("data.maxLng");
        Constants.MAX_LATITUDE = this.config.getDouble("data.maxLat");

        Constants.MIN_X = SuperCluster.lngX(Constants.MIN_LONGITUDE);
        Constants.MIN_Y = SuperCluster.latY(Constants.MAX_LATITUDE); // latitude -> x is reversed than geo coordinates
        Constants.MAX_X = SuperCluster.lngX(Constants.MAX_LONGITUDE);
        Constants.MAX_Y = SuperCluster.latY(Constants.MIN_LATITUDE); // latitude -> x is reversed than geo coordinates

        Constants.MAX_RESOLUTION = this.config.getInt("index.maxResolution");
    }

    public static Props getProps() {
        return Props.create(Agent.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(JsonNode.class, request -> {
                    MyLogger.info(this.getClass(), "received request: " + request);
                    if (this.out == null) {
                        this.out = sender();
                    }
                    handleRequest(request);
                })
                .build();
    }

    private void buildGeoJsonArrayCluster(Cluster[] clusters, ArrayNode geoJsonArray) {
        for (int i = 0; i < clusters.length; i ++) {
            ObjectNode feature = JsonNodeFactory.instance.objectNode();
            feature.put("type", "Feature");

            ObjectNode properties = JsonNodeFactory.instance.objectNode();
            properties.put("point_count", clusters[i].numPoints);
            properties.put("point_count_abbreviated", clusters[i].numPoints);
            properties.put("id", clusters[i].getId());
            properties.put("expansionZoom", clusters[i].expansionZoom);
            feature.set("properties", properties);

            ObjectNode geometry = JsonNodeFactory.instance.objectNode();
            ArrayNode coordinates = geometry.putArray("coordinates");
            coordinates.add(clusters[i].getX());
            coordinates.add(clusters[i].getY());
            geometry.put("type", "Point");
            feature.set("geometry", geometry);

            geoJsonArray.add(feature);
        }
    }

    private void buildGeoJsonArrayPoint(List<PointTuple> points, ArrayNode geoJsonArray) {
        for (int i = 0; i < points.size(); i ++) {
            ObjectNode feature = JsonNodeFactory.instance.objectNode();
            feature.put("type", "Feature");

            ObjectNode properties = JsonNodeFactory.instance.objectNode();
            feature.set("properties", properties);

            ObjectNode geometry = JsonNodeFactory.instance.objectNode();
            ArrayNode coordinates = geometry.putArray("coordinates");
            coordinates.add(points.get(i).getX());
            coordinates.add(points.get(i).getY());
            geometry.put("type", "Point");
            feature.set("geometry", geometry);

            geoJsonArray.add(feature);
        }
    }

    private void buildDataArrayPoint(List<PointTuple> points, ArrayNode dataArray) {
        for (int i = 0; i < points.size(); i ++) {
            ArrayNode pointTuple = JsonNodeFactory.instance.arrayNode();
            pointTuple.add(points.get(i).getX());
            pointTuple.add(points.get(i).getY());
            pointTuple.add(points.get(i).getId());
            dataArray.add(pointTuple);
        }
    }

    /**
     * handle query request
     *  - if given cluster key does NOT exists,
     *      do the loadData and clusterData first,
     *  - query the cluster
     *
     * @param _request
     */
    private void handleQuery(Request _request) {

        if (_request.query == null) {
            // TODO - exception
        }
        Query query = _request.query;

        if (query.cluster == null) {
            // TODO - exception
        }
        String clusterKey = query.cluster;


        // handle non-progressive query
        if (query.algorithm == null || query.algorithm.equalsIgnoreCase("SuperCluster") || query.algorithm.equalsIgnoreCase("sc")) {

            // if given cluster key does NOT exists, do the loadData and clusterData first,
            if (!superClusters.containsKey(clusterKey)) {
                if (_request.keyword == null) {
                    // TODO - exception
                }
                boolean success = loadData(_request.keyword);
                if (!success) {
                    // TODO - exception
                }

                String clusterOrder = query.order == null ? "original" : query.order;
                String indexType = query.indexType == null ? "KDTree" : query.indexType;
                success = clusterData(clusterKey, clusterOrder, "SuperCluster", indexType, false, false, false);
                if (!success) {
                    // TODO - exception
                }
            }

            answerClusterQuery(clusterKey, _request, "done", 100);
        }
        // handle progressive query
        else {
            // if given cluster key does NOT exists, do the loadData and clusterData first,
            if (!superClusters.containsKey(clusterKey)) {
                handleQueryProgressively(_request);
            }
            // otherwise, answer the query directly
            else {
                answerClusterQuery(clusterKey, _request, "done", 100);
            }
        }
    }

    private void answerClusterQuery(String clusterKey, Request _request, String status, int progress) {
        Query query = _request.query;

        // Add hit to querying super cluster
        superClustersHits.put(clusterKey, superClustersHits.get(clusterKey) + 1);

        // query the cluster
        SuperCluster cluster = superClusters.get(clusterKey);

        Cluster[] clusters;
        if (query.bbox == null) {
            clusters = cluster.getClusters(query.zoom);
        } else {
            clusters = cluster.getClusters(query.bbox[0], query.bbox[1], query.bbox[2], query.bbox[3], query.zoom);
        }

        // construct the response Json and return
        JsonNode response = Json.toJson(_request);

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        ArrayNode data = result.putArray("data");

        if (clusters != null) {
            buildGeoJsonArrayCluster(clusters, data);
        }

        ((ObjectNode) response).put("status", status);
        ((ObjectNode) response).put("progress", progress);
        ((ObjectNode) response).set("result", result);
        respond(response);
    }

    private void handleQueryProgressively(Request _request) {

        // for experiments analysis
        MyTimer.progressTimer.clear();
        MyMemory.progressUsedMemory.clear();
        MyMemory.porgressTotalMemory.clear();

        Query query = _request.query;
        String clusterKey = query.cluster;
        if (_request.keyword == null) {
            // TODO - exception
        }
        this.pointTuples = null;
        Date currentStart = new Date(this.start.getTime());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentStart);
        calendar.add(Calendar.DATE, this.intervalDays);
        Date currentEnd = calendar.getTime();
        long totalDays = (this.end.getTime() - this.start.getTime()) / (24 * 3600 * 1000);
        List<Double> randIndexes = new ArrayList<>();
        while (currentStart.before(this.end)) {

            long progress = (currentEnd.getTime() - this.start.getTime()) / (24 * 3600 * 1000);
            progress = 100 * progress / totalDays;

            // true - if pointTuples only keep delta data, false - otherwise
            boolean deltaOnly = true;
            if (query.algorithm.equalsIgnoreCase("SuperCluster") || query.algorithm.equalsIgnoreCase("sc")
                    || query.algorithm.equalsIgnoreCase("SuperClusterInBatch") || query.algorithm.equalsIgnoreCase("scib")) {
                deltaOnly = false;
            }
            boolean success = loadNewData(_request.keyword, currentStart, currentEnd, deltaOnly);
            if (!success) {
                // TODO - exception
            }

            String clusterOrder = query.order == null ? "original" : query.order;
            String indexType = query.indexType == null ? "KDTree" : query.indexType;
            MyTimer.startTimer();
            success = clusterData(clusterKey, clusterOrder, query.algorithm, indexType, deltaOnly, _request.analysis != null, query.preCluster);
            MyTimer.stopTimer();
            MyTimer.progressTimer.add(MyTimer.durationSeconds());
            MyMemory.progressUsedMemory.add(MyMemory.getUsedMemory());
            MyMemory.porgressTotalMemory.add(MyMemory.getTotalMemory());
            if (!success) {
                // TODO - exception
            }

            answerClusterQuery(clusterKey, _request, "in-progress", (int) progress);

            if (_request.analysis != null) {
                Analysis analysis = _request.analysis;
                if (analysis.arguments.length != 3) {
                    // TODO - exception
                }
                String clusterKey1 = analysis.arguments[0];
                String clusterKey2 = analysis.arguments[1];
                int zoom = Integer.valueOf(analysis.arguments[2]);

                boolean adjusted = analysis.objective.equalsIgnoreCase("adjusted-rand-index")? true: false;
                double randIndex = randIndex(clusterKey1, clusterKey2, zoom, adjusted);
                randIndexes.add(randIndex);

                JsonNode response = Json.toJson(_request);
                response = Json.toJson(_request);
                ((ObjectNode) response).put("type", "analysis");
                ((ObjectNode) response).put("id", "console");
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("randIndex", randIndex);
                ((ObjectNode) response).put("status", "done");
                ((ObjectNode) response).set("result", result);
                respond(response);
            }

            currentStart = currentEnd;
            calendar = Calendar.getInstance();
            calendar.setTime(currentStart);
            calendar.add(Calendar.DATE, this.intervalDays);
            currentEnd = calendar.getTime();
        }

        // finally output the sequence of rand index values in one response
        if (_request.analysis != null) {
            JsonNode response = Json.toJson(_request);
            ((ObjectNode) response).put("type", "analysis");
            ((ObjectNode) response).put("id", "console");
            ArrayNode result = ((ObjectNode) response).putArray("result");
            for (Double randIndex : randIndexes) {
                result.add(randIndex);
            }
            ((ObjectNode) response).put("status", "done");
            respond(response);
        }

        // for experiments analysis
        System.out.println("========== Experiment Analysis ==========");
        System.out.println("Progressive Query: ");
        System.out.println("keyword: " + _request.keyword);
        System.out.println("algorithm: " + _request.query.algorithm);
        System.out.println("clustering time for each batch: ");
        for (double time: MyTimer.progressTimer) {
            System.out.println(time);
        }
        System.out.println("memory usage until each batch (MB): ");
        for (int i = 0; i < MyMemory.progressUsedMemory.size(); i ++) {
            System.out.println(MyMemory.progressUsedMemory.get(i) + ",  " + MyMemory.porgressTotalMemory.get(i));
        }
        if (_request.analysis != null) {
            System.out.println("Rand index values for each batch: ");
            for (double randIndex : randIndexes) {
                System.out.println(randIndex);
            }
        }
        System.out.println("========== =================== ==========");
    }

    private void handleCmds(Request _request) {
        Command[] cmds = _request.cmds;

        if (cmds.length == 0) {
            respond(buildCmdResponse(_request, "null", "cmds is empty", "done"));
        }
        else {
            // run commands one by one, if any command fails, stop
            for (int i = 0; i < cmds.length; i ++) {
                Command cmd = cmds[i];
                boolean success = handleCmd(cmd, _request);
                if (!success) {
                    break;
                }
            }
        }
    }

    /**
     * load data for given keyword
     *
     * @return
     */
    private boolean loadData(String keyword) {
        if (this.keyword != null && this.keyword.equals(keyword)) {
            return true;
        }
        if (postgreSQL == null) {
            postgreSQL = new PostgreSQL();
        }
        pointTuples = postgreSQL.queryPointTuplesForKeyword(keyword);
        if (pointTuples == null) {
            return false;
        }
        for (int i = 0; i < pointTuples.size(); i ++) {
            pointTuples.get(i).setId(i);
        }
        return true;
    }

    /**
     * load new data for given keyword and time range
     *
     * @param deltaOnly - true - keep delta only in this.pointTuples, false - append to the end of this.pointTuples
     * @return
     */
    private boolean loadNewData(String keyword, Date start, Date end, boolean deltaOnly) {
        if (postgreSQL == null) {
            postgreSQL = new PostgreSQL();
        }
        List<PointTuple> deltaPointTuples;
        if (keyword.equals("%")) {
            deltaPointTuples = postgreSQL.queryPointTuplesForTime(start, end);
        }
        else {
            deltaPointTuples = postgreSQL.queryPointTuplesForKeywordAndTime(keyword, start, end);
        }
        if (deltaPointTuples == null) {
            return false;
        }
        for (int i = 0; i < deltaPointTuples.size(); i ++) {
            deltaPointTuples.get(i).setId(i);
        }
        if (pointTuples == null || deltaOnly) {
            pointTuples = deltaPointTuples;
        }
        else {
            pointTuples.addAll(deltaPointTuples);
        }
        return true;
    }

    /**
     * cluster data for given order and name it with given key
     *
     * @param clusterKey
     * @param clusterOrder
     * @param deltaOnly - valid only for progressive queries, true - if pointTuples only keep delta data, false - otherwise
     * @param analysis - true - if we need to analysis the rand-index of clustering results, false - otherwise
     * @return
     */
    private boolean clusterData(String clusterKey, String clusterOrder, String algorithm, String indexType, boolean deltaOnly, boolean analysis, boolean preCluster) {

        if (analysis) {
            double[][] points = orderPoints(clusterKey, clusterOrder, deltaOnly);
            if (points == null) {
                return false;
            }
            else {
                SuperCluster cluster = getSuperCluster(clusterKey, algorithm, indexType, true);
                cluster.load(points);
                if (deltaOnly) {
                    PointTupleListFactory.recycle(this.pointTuples);
                }
            }
        }
        // when no analysis, use pointTuples list directly for loading
        else {
            if (this.pointTuples == null || this.pointTuples.isEmpty()) {
                return false;
            }
            else {
                SuperCluster cluster = getSuperCluster(clusterKey, algorithm, indexType, false);
                cluster.load(this.pointTuples, preCluster);
                if (deltaOnly) {
                    PointTupleListFactory.recycle(this.pointTuples);
                }
            }
        }

        return true;
    }

    private SuperCluster getSuperCluster(String clusterKey, String algorithm, String indexType, boolean analysis) {
        SuperCluster cluster;
        if (superClusters.containsKey(clusterKey)) {
            cluster = superClusters.get(clusterKey);
        }
        else {
            // if too many cached clusters, replace the least used one
            if (superClusters.size() > MAX_CLUSTERS) {
                String leastUsedClusterKey = null;
                int leastHit = Integer.MAX_VALUE;
                for (Map.Entry<String, Integer> map: this.superClustersHits.entrySet()) {
                    if (map.getValue() < leastHit) {
                        leastHit = map.getValue();
                        leastUsedClusterKey = map.getKey();
                    }
                }
                if (leastUsedClusterKey != null) {
                    superClusters.remove(leastUsedClusterKey);
                }
            }

            switch (algorithm.toLowerCase()) {
                case "isupercluster":
                case "isc":
                    cluster = new iSuperCluster(this.minZoom, this.maxZoom, analysis);
                    break;
                case "aisupercluster":
                case "aisc":
                    cluster = new AiSuperCluster(this.minZoom, this.maxZoom, analysis);
                    break;
                case "bisupercluster":
                case "bisc":
                    cluster = new BiSuperCluster(this.minZoom, this.maxZoom, indexType, analysis);
                    break;
                case "lbisupercluster":
                case "lbisc":
                    cluster = new LBiSuperCluster(this.minZoom, this.maxZoom, indexType, analysis);
                    break;
                case "sbisupercluster":
                case "sbic":
                    cluster = new SBiSuperCluster(this.minZoom, this.maxZoom, indexType, analysis);
                    break;
                default:
                    cluster = new SuperCluster(this.minZoom, this.maxZoom, indexType);
            }
            superClusters.put(clusterKey, cluster);
            superClustersHits.put(clusterKey, 0);
        }
        return cluster;
    }

    private boolean handleCmd(Command _cmd, Request _request) {
        boolean success = true;
        switch (_cmd.action) {
            case "load":
                success = loadData(_request.keyword);
                if (success) {
                    respond(buildCmdResponse(_request, _cmd.action, "data loaded, size = " + this.pointTuples.size(), "done"));
                }
                else {
                    respond(buildCmdResponse(_request, _cmd.action, "load data failed", "error"));
                    return false;
                }
                break;
            case "cluster":
                if (_cmd.arguments.length != 2) {
                    respond(buildCmdResponse(_request, _cmd.action, "must contain 2 arguments: key, order", "error"));
                    return false;
                }
                else {
                    String clusterKey = _cmd.arguments[0];
                    String clusterOrder = _cmd.arguments[1];
                    success = clusterData(clusterKey, clusterOrder, "SuperCluster", "KDTree", false, false, false);
                    if (success) {
                        respond(buildCmdResponse(_request, _cmd.action, "cluster built for key = " + clusterKey + " order = " + clusterOrder, "done"));
                    }
                    else {
                        respond(buildCmdResponse(_request, _cmd.action, "argument for order is unknown", "error"));
                        return false;
                    }
                }
                break;
            default:
                respond(buildCmdResponse(_request, _cmd.action, "action is unknown", "error"));
                return false;
        }
        return true;
    }

    private double[][] orderPoints(String _key, String _order, boolean deltaOnly) {
        double[][] points = new double[pointTuples.size()][2];
        Long[] orderMap = new Long[points.length];
        switch (_order) {
            case "original":
                for (int i = 0; i < pointTuples.size(); i ++) {
                    points[i][0] = pointTuples.get(i).getX();
                    points[i][1] = pointTuples.get(i).getY();
                    orderMap[i] = pointTuples.get(i).tid;
                }
                break;
            case "reverse":
                for (int i = 0; i < pointTuples.size(); i ++) {
                    points[pointTuples.size() - 1 - i][0] = pointTuples.get(i).getX();
                    points[pointTuples.size() - 1 - i][1] = pointTuples.get(i).getY();
                    orderMap[pointTuples.size() - 1 - i] = pointTuples.get(i).tid;
                }
                break;
            case "spatial":
                //TODO - verify the ordering since we get rid of the array <-> list
                Collections.sort(pointTuples, PointTuple.getSpatialComparator());
                for (int i = 0; i < pointTuples.size(); i ++) {
                    points[i][0] = pointTuples.get(i).getX();
                    points[i][1] = pointTuples.get(i).getY();
                    orderMap[i] = pointTuples.get(i).tid;
                }
                break;
            case "reverse-spatial":
                //TODO - verify the ordering since we get rid of the array <-> list
                Collections.sort(pointTuples, PointTuple.getReverseSpatialComparator());
                for (int i = 0; i < pointTuples.size(); i ++) {
                    points[i][0] = pointTuples.get(i).getX();
                    points[i][1] = pointTuples.get(i).getY();
                    orderMap[i] = pointTuples.get(i).tid;
                }
                break;
            default:
                return null;
        }
        // if already has order maps for this cluster key and this.pointTuples in deltaOnly mode
        // append the orderMap to existing orderMap
        if (orderMaps.containsKey(_key) && deltaOnly) {
            Long[] curOrderMap = orderMaps.get(_key);
            List<Long> base = new ArrayList(Arrays.asList(curOrderMap));
            base.addAll(Arrays.asList(orderMap));
            orderMap = base.toArray(new Long[base.size()]);
        }
        orderMaps.put(_key, orderMap);
        return points;
    }

    /**
     * Calculate rand index between given (clusterKey1/2) two clustering results
     * for data points within given range ([x0, y0] - [x1, y1]) at given zoom level
     *
     * @param clusterKey1
     * @param clusterKey2
     * @param zoom
     * @return
     */
    private double randIndex(String clusterKey1, String clusterKey2, int zoom, boolean adjusted) {
        if (!superClusters.containsKey(clusterKey1) || !superClusters.containsKey(clusterKey2)) {
            // TODO - exception
            return 0.0;
        }

        int[] labels1 = superClusters.get(clusterKey1).getClusteringLabels(zoom);
        int[] labels2 = superClusters.get(clusterKey2).getClusteringLabels(zoom);

        //-DEBUG-//
//        System.out.println("labels1 [" + labels1.length + "] = " + Arrays.toString(labels1));
//        System.out.println("labels2 [" + labels2.length + "] = " + Arrays.toString(labels2));

        // one cluster only has partial data,
        // make labels1/clusterKey1 always point to the smaller one,
        // and labels2/clusterKey2 always point to the larger one
        if (labels1.length > labels2.length) {
            int[] labelsSwap = labels1;
            labels1 = labels2;
            labels2 = labelsSwap;

            String clusterKeySwap = clusterKey1;
            clusterKey1 = clusterKey2;
            clusterKey2 = clusterKeySwap;
        }

        // build hash map from tid to array index of labels1 (cluster result 1)
        Map<Long, Integer> tidMap1 = new HashMap<>();
        Long[] orderMap1 = orderMaps.get(clusterKey1);
        for (int i = 0;i < labels1.length; i ++) {
            tidMap1.put(orderMap1[i], i);
        }

        // build labels array with the same tid order as labels1
        int[] newLabels2 = new int[labels1.length];
        Long[] orderMap2 = orderMaps.get(clusterKey2);
        for (int i = 0; i < orderMap2.length; i ++) {
            Long tid = orderMap2[i];
            if (tidMap1.containsKey(tid)) {
                int index = tidMap1.get(tid);
                newLabels2[index] = labels2[i];
            }
        }
        labels2 = newLabels2;

        if (adjusted) {
            return RandIndex.adjustedRandIndex(labels1, labels2);
        }
        else {
            return RandIndex.randIndex(labels1, labels2);
        }
    }

    private JsonNode buildCmdResponse(Request _request, String _cursor, String _msg, String _status) {
        JsonNode response = Json.toJson(_request);
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("cursor", _cursor);
        result.put("message", _msg);
        ((ObjectNode) response).put("status", _status);
        ((ObjectNode) response).set("result", result);
        return response;
    }

    private void handleTransfer(Request _request) {
        if (_request.keyword == null) {
            // TODO - exception
        }
        keyword = _request.keyword;
        if (postgreSQL == null) {
            postgreSQL = new PostgreSQL();
        }
        pointTuples = postgreSQL.queryPointTuplesForKeyword(_request.keyword);
        if (pointTuples == null) {
            // TODO - exception
        }
        for (int i = 0; i < pointTuples.size(); i ++) {
            pointTuples.get(i).setId(i);
        }
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("type", "FeatureCollection");
        ArrayNode features = result.putArray("features");
        buildGeoJsonArrayPoint(pointTuples, features);
        respond(result);
    }

    private void handleProgressTransfer(Request _request) {
        if (_request.keyword == null) {
            // TODO - exception
        }

        this.pointTuples = null;
        Date currentStart = new Date(this.start.getTime());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentStart);
        calendar.add(Calendar.DATE, this.intervalDays);
        Date currentEnd = calendar.getTime();
        long totalDays = (this.end.getTime() - this.start.getTime()) / (24 * 3600 * 1000);

        while (currentStart.before(this.end)) {

            long progress = (currentEnd.getTime() - this.start.getTime()) / (24 * 3600 * 1000);
            progress = 100 * progress / totalDays;

            // query delta data, pointTuples only keep delta data
            boolean success = loadNewData(_request.keyword, currentStart, currentEnd, true);
            if (!success) {
                // TODO - exception
            }

            // construct the response Json and return
            JsonNode response = Json.toJson(_request);
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            ArrayNode data = result.putArray("data");
            buildDataArrayPoint(pointTuples, data);
            ((ObjectNode) response).put("progress", progress);
            ((ObjectNode) response).set("result", result);
            respond(response);

            currentStart = currentEnd;
            calendar = Calendar.getInstance();
            calendar.setTime(currentStart);
            calendar.add(Calendar.DATE, this.intervalDays);
            currentEnd = calendar.getTime();
        }
    }

    private void handleAnalysis(Request _request) {
        if (_request.analysis.objective == null) {
            // TODO - exception
        }
        switch (_request.analysis.objective) {
            case "distance":
                if (_request.keyword == null) {
                    // TODO - exception
                }
                keyword = _request.keyword;
                if (_request.analysis == null) {
                    // TODO - exception
                }
                Analysis analysis = _request.analysis;
                if (analysis.arguments.length != 4) {
                    // TODO - exception
                }

                String clusterKey = analysis.arguments[0];
                int zoom = Integer.valueOf(analysis.arguments[1]);
                int p1 = Integer.valueOf(analysis.arguments[2]);
                int p2 = Integer.valueOf(analysis.arguments[3]);

                if (!superClusters.containsKey(clusterKey)) {
                    // TODO - exception
                }
                SuperCluster cluster = superClusters.get(clusterKey);

                double distance = cluster.getClusterDistance(zoom, p1, p2);
                double radius = cluster.getRadius(zoom);

                JsonNode response = Json.toJson(_request);
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("distance", distance);
                result.put("radius", radius);
                ((ObjectNode) response).put("status", "done");
                ((ObjectNode) response).set("result", result);
                respond(response);
                break;
            case "rand-index":
                if (_request.analysis == null) {
                    // TODO - exception
                }
                analysis = _request.analysis;
                if (analysis.arguments.length != 3) {
                    // TODO - exception
                }
                String clusterKey1 = analysis.arguments[0];
                String clusterKey2 = analysis.arguments[1];
                zoom = Integer.valueOf(analysis.arguments[2]);

                double randIndex = randIndex(clusterKey1, clusterKey2, zoom, false);

                response = Json.toJson(_request);
                result = JsonNodeFactory.instance.objectNode();
                result.put("randIndex", randIndex);
                ((ObjectNode) response).put("status", "done");
                ((ObjectNode) response).set("result", result);
                respond(response);
                break;
        }

    }

    private void handleRequest(JsonNode _request) {
        Request request = Json.fromJson(_request, Request.class);

        switch (request.type) {
            case "query":
                MyLogger.info(this.getClass(), "request is a Query");
                handleQuery(request);
                break;
            case "cmd":
                MyLogger.info(this.getClass(), "request is a Command");
                handleCmds(request);
                break;
            case "transfer":
                MyLogger.info(this.getClass(), "request is a Transfer");
                handleTransfer(request);
                break;
            case "progress-transfer":
                MyLogger.info(this.getClass(), "request is a Progress-Transfer");
                handleProgressTransfer(request);
                break;
            case "analysis":
                MyLogger.info(this.getClass(), "request is a Analysis");
                handleAnalysis(request);
                break;
            default:
                MyLogger.info(this.getClass(), "request type is unknown");
                JsonNode response = Json.toJson(_request);
                ((ObjectNode) response).put("status", "unknown");
                ((ObjectNode) response).put("message", "this request type is unknown");
                respond(response);
        }
    }

    private void respond(JsonNode _response) {
        out.tell(_response, self());
    }
}
