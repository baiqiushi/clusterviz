package clustering;

import model.Cluster;
import model.PointTuple;
import util.I2DIndex;
import util.IndexCreator;
import util.MyTimer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreSuperCluster extends SuperCluster {

    //-Timing-//
    static final boolean keepTiming = true;
    Map<String, Double> timing;
    //-Timing-//

    public PreSuperCluster() {
        // initialize the timing map
        if (keepTiming) {
            timing = new HashMap<>();
            timing.put("total", 0.0);
            timing.put("createClusters", 0.0);
            timing.put("buildIndex", 0.0);
            timing.put("runCluster", 0.0);
        }
    }

    /**
     * preCluster the raw data points into clusters for (maxZoom + 1) level
     *
     * @param points
     * @return
     */
    public List<Cluster> preCluster(List<PointTuple> points) {
        if (keepTiming) MyTimer.startTimer();

        if (keepTiming) MyTimer.startTimer();
        // generate a cluster object for each point
        Cluster[] pointClusters = createPointClusters(points);
        if (keepTiming) MyTimer.stopTimer();
        if (keepTiming) timing.put("createClusters", timing.get("createClusters") + MyTimer.durationSeconds());

        if (keepTiming) MyTimer.startTimer();
        // index input points into a KD-tree
        I2DIndex<Cluster> pointsIndex = IndexCreator.createIndex(this.indexType, getRadius(maxZoom + 1));
        pointsIndex.load(pointClusters);
        if (keepTiming) MyTimer.stopTimer();
        if (keepTiming) timing.put("buildIndex", timing.get("buildIndex") + MyTimer.durationSeconds());

        if (keepTiming) MyTimer.startTimer();
        List<Cluster> result = _clusters(pointClusters, maxZoom + 1, pointsIndex);
        if (keepTiming) MyTimer.stopTimer();
        if (keepTiming) timing.put("runCluster", timing.get("runCluster") + MyTimer.durationSeconds());

        if (keepTiming) MyTimer.stopTimer();
        if (keepTiming) timing.put("total", timing.get("total") + MyTimer.durationSeconds());
        return result;
    }

    /**
     * the same logic with SuperCluster._clusters(),
     * differences are:
     *    (1) instead of using this.indexes[zoom + 1],
     *         here use the pointsIndex given in the argument;
     *    (2) here we don't keep children of cluster,
     *         but only calculate the centroid, and update numPoints;
     *
     * @param points
     * @param zoom
     * @param pointsIndex
     * @return
     */
    private List<Cluster> _clusters(Cluster[] points, int zoom, I2DIndex<Cluster> pointsIndex) {
        List<Cluster> clusters = new ArrayList<>();

        double r = getRadius(zoom);

        // loop through each point
        for (int i = 0; i < points.length; i ++) {
            Cluster p = points[i];

            // if we've already visited the point at this zoom level, skip it
            if (p.zoom <= zoom) {
                continue;
            }
            p.zoom = zoom;

            // find all nearby points
            List<Cluster> neighbors = pointsIndex.within(p, r);

            int numPoints = p.numPoints == 0? 1: p.numPoints;
            double wx = p.getX() * numPoints;
            double wy = p.getY() * numPoints;

            // encode both zoom and point index on which the cluster originated
            int id = (i << 5) + (zoom + 1);

            for (Cluster neighbor : neighbors) {

                // filter out neighbors that are already processed or the point itself
                if (neighbor.zoom <= zoom) {
                    continue;
                }
                neighbor.zoom = zoom; // save the zoom (so it doesn't get processed twice)

                int numPoints2 = neighbor.numPoints == 0? 1: neighbor.numPoints;
                // accumulate coordinates for calculating weighted center
                wx += neighbor.getX() * numPoints2;
                wy += neighbor.getY() * numPoints2;

                numPoints += numPoints2;
                neighbor.parentId = id;
            }

            p.setX(wx / numPoints);
            p.setY(wy / numPoints);
            p.setId(id);
            p.numPoints = numPoints;

            clusters.add(p);
        }

        return clusters;
    }

    public void printTiming() {
        System.out.println("------------------------ Pre Cluster ------------------------");
        System.out.println("[Total Time] " + timing.get("total") + " seconds.");
        System.out.println("    [create clusters] " + timing.get("createClusters") + " seconds");
        System.out.println("    [build index] " + timing.get("buildIndex") + " seconds");
        System.out.println("    [run cluster] " + timing.get("runCluster") + " seconds");
        System.out.println("-------------------------------------------------------------");
    }
}
