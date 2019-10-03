package actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import clustering.SuperCluster;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import model.*;
import play.libs.Json;
import util.MyLogger;
import util.PostgreSQL;

import javax.inject.Inject;
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
    private PointTuple[] pointTuples;
    private Map<String, SuperCluster> superClusters;

    @Inject
    public Agent() {
        this.out = null;
        this.config = null;
        this.superClusters = new HashMap<>();
    }

    @Inject
    public Agent(ActorRef out, Config config) {
        this.out = out;
        this.config = config;
        this.superClusters = new HashMap<>();
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
            properties.put("id", clusters[i].id);
            feature.set("properties", properties);

            ObjectNode geometry = JsonNodeFactory.instance.objectNode();
            ArrayNode coordinates = geometry.putArray("coordinates");
            coordinates.add(clusters[i].getDimensionValue(0));
            coordinates.add(clusters[i].getDimensionValue(1));
            geometry.put("type", "Point");
            feature.set("geometry", geometry);

            geoJsonArray.add(feature);
        }
    }

    private void buildGeoJsonArrayPoint(Point[] points, ArrayNode geoJsonArray) {
        for (int i = 0; i < points.length; i ++) {
            ObjectNode feature = JsonNodeFactory.instance.objectNode();
            feature.put("type", "Feature");

            ObjectNode properties = JsonNodeFactory.instance.objectNode();
            feature.set("properties", properties);

            ObjectNode geometry = JsonNodeFactory.instance.objectNode();
            ArrayNode coordinates = geometry.putArray("coordinates");
            coordinates.add(points[i].getDimensionValue(0));
            coordinates.add(points[i].getDimensionValue(1));
            geometry.put("type", "Point");
            feature.set("geometry", geometry);

            geoJsonArray.add(feature);
        }
    }

    private void handleQuery(Request _request) {

        if (_request.query == null) {
            // TODO - exception
        }
        Query query = _request.query;

        if (query.cluster == null) {
            // TODO - exception
        }
        String clusterKey = query.cluster;

        if (!superClusters.containsKey(clusterKey)) {
            // TODO - exception
        }
        SuperCluster cluster = superClusters.get(clusterKey);

        Cluster[] clusters;
        if (query.bbox == null) {
            clusters = cluster.getClusters(query.zoom);
        }
        else {
            clusters = cluster.getClusters(query.bbox[0], query.bbox[1], query.bbox[2], query.bbox[3], query.zoom);
        }

        JsonNode response = Json.toJson(_request);

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        ArrayNode data = result.putArray("data");

        buildGeoJsonArrayCluster(clusters, data);

        ((ObjectNode) response).put("status", "done");
        ((ObjectNode) response).set("result", result);
        respond(response);
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

    private boolean handleCmd(Command _cmd, Request _request) {
        switch (_cmd.action) {
            case "load":
                keyword = _request.keyword;
                if (postgreSQL == null) {
                    postgreSQL = new PostgreSQL();
                }
                pointTuples = postgreSQL.queryPointTuplesForKeyword(keyword);
                if (pointTuples == null) {
                    respond(buildCmdResponse(_request, _cmd.action, "load data failed", "error"));
                    return false;
                }
                for (int i = 0; i < pointTuples.length; i ++) {
                    pointTuples[i].id = i;
                }
                respond(buildCmdResponse(_request, _cmd.action, "data loaded, size = " + pointTuples.length, "done"));
                break;
            case "cluster":
                if (_cmd.arguments.length != 2) {
                    respond(buildCmdResponse(_request, _cmd.action, "must contain 2 arguments: key, order", "error"));
                    return false;
                }
                else {
                    String clusterKey = _cmd.arguments[0];
                    String clusterOrder = _cmd.arguments[1];
                    Double[][] points = orderPoints(clusterOrder);
                    if (points == null) {
                        respond(buildCmdResponse(_request, _cmd.action, "argument for order is unknown", "error"));
                        return false;
                    }
                    else {
                        SuperCluster cluster;
                        if (superClusters.containsKey(clusterKey)) {
                            cluster = superClusters.get(clusterKey);
                        }
                        else {
                            cluster = new SuperCluster();
                            superClusters.put(clusterKey, cluster);
                        }
                        cluster.load(points);
                        respond(buildCmdResponse(_request, _cmd.action, "cluster built for key = " + clusterKey + " order = " + clusterOrder, "done"));
                    }
                }
                break;
            default:
                respond(buildCmdResponse(_request, _cmd.action, "action is unknown", "error"));
                return false;
        }
        return true;
    }

    private Double[][] orderPoints(String _order) {
        Double[][] points = new Double[pointTuples.length][2];
        switch (_order) {
            case "original":
                for (int i = 0; i < pointTuples.length; i ++) {
                    points[i][0] = pointTuples[i].getDimensionValue(0);
                    points[i][1] = pointTuples[i].getDimensionValue(1);
                }
                break;
            case "reverse":
                for (int i = 0; i < pointTuples.length; i ++) {
                    points[pointTuples.length - 1 - i][0] = pointTuples[i].getDimensionValue(0);
                    points[pointTuples.length - 1 - i][1] = pointTuples[i].getDimensionValue(1);
                }
                break;
            case "spatial":
                List<PointTuple> pointTuplesList = Arrays.asList(pointTuples);
                Collections.sort(pointTuplesList, new Comparator<PointTuple>() {
                    public int compare(PointTuple p1, PointTuple p2) {
                        if (p1.getDimensionValue(0) == p2.getDimensionValue(0)
                                && p1.getDimensionValue(1) == p2.getDimensionValue(1)) {
                            return 0;
                        }
                        else if (p1.getDimensionValue(0) == p2.getDimensionValue(0)) {
                            if (p1.getDimensionValue(1) < p2.getDimensionValue(1)) {
                                return -1;
                            }
                            else {
                                return 1;
                            }
                        }
                        else if (p1.getDimensionValue(1) == p2.getDimensionValue(1)) {
                            if (p1.getDimensionValue(0) < p2.getDimensionValue(0)) {
                                return -1;
                            }
                            else {
                                return 1;
                            }
                        }
                        else {
                            if (p1.getDimensionValue(0) < p2.getDimensionValue(0)) {
                                return -1;
                            }
                            else {
                                return 1;
                            }
                        }
                    }
                });
                PointTuple[] cPointTuples = pointTuplesList.toArray(new PointTuple[pointTuplesList.size()]);
                for (int i = 0; i < cPointTuples.length; i ++) {
                    points[i][0] = cPointTuples[i].getDimensionValue(0);
                    points[i][1] = cPointTuples[i].getDimensionValue(1);
                }
                break;
            default:
                return null;
        }
        return points;
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
        pointTuples = postgreSQL.queryPointTuplesForKeyword(keyword);
        if (pointTuples == null) {
            // TODO - exception
        }
        for (int i = 0; i < pointTuples.length; i ++) {
            pointTuples[i].id = i;
        }
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("type", "FeatureCollection");
        ArrayNode features = result.putArray("features");
        buildGeoJsonArrayPoint(pointTuples, features);
        respond(result);
    }

    private void handleAnalysis(Request _request) {
        if (_request.keyword == null) {
            // TODO - exception
        }
        keyword = _request.keyword;
        if (_request.analysis == null) {
            // TODO - exception
        }
        Analysis analysis = _request.analysis;

        if (analysis.cluster == null) {
            // TODO - exception
        }
        String clusterKey = analysis.cluster;

        if (!superClusters.containsKey(clusterKey)) {
            // TODO - exception
        }
        SuperCluster cluster = superClusters.get(clusterKey);

        double distance = cluster.getClusterDistance(analysis.zoom, analysis.p1, analysis.p2);
        double radius = cluster.getRadius(analysis.zoom);

        JsonNode response = Json.toJson(_request);
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("distance", distance);
        result.put("radius", radius);
        ((ObjectNode) response).put("status", "done");
        ((ObjectNode) response).set("result", result);
        respond(response);
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
