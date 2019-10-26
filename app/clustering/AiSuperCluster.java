package clustering;

import model.Advocator;
import model.Cluster;
import util.KDTree;

import java.util.ArrayList;
import java.util.List;

public class AiSuperCluster extends SuperCluster {

    KDTree<Advocator>[] advocatorsTrees;
    List<Cluster>[] advocatorClusters;
    int pointIdSeq;
    int advocatorSeq;

    public AiSuperCluster() {
        this.advocatorsTrees = new KDTree[maxZoom + 1];
        this.advocatorClusters = new List[maxZoom + 1];
        this.pointIdSeq = 0;
        this.advocatorSeq = 0;

        for (int i = 0; i <= maxZoom; i ++) {
            this.advocatorsTrees[i] = new KDTree<>(K);
            this.advocatorClusters[i] = new ArrayList<>();
        }
    }

    public AiSuperCluster(int _minZoom, int _maxZoom) {
        this.minZoom = _minZoom;
        this.maxZoom = _maxZoom;
        this.advocatorsTrees = new KDTree[maxZoom + 1];
        this.advocatorClusters = new List[maxZoom + 1];
        this.pointIdSeq = 0;
        this.advocatorSeq = 0;

        for (int i = 0; i <= maxZoom; i ++) {
            this.advocatorsTrees[i] = new KDTree<>(K);
            this.advocatorClusters[i] = new ArrayList<>();
        }
    }

    public void load(double[][] points) {
        System.out.println("Advocator incremental SuperCluster loading " + points.length + " points ... ...");
        long start = System.nanoTime();

        this.totalNumberOfPoints += points.length;

        // insert points to max zoom level one by one
        for (int i = 0; i < points.length; i ++) {
            insert(createPointCluster(points[i], this.pointIdSeq ++), maxZoom);
        }

        long end = System.nanoTime();
        System.out.println("incremental SuperCluster loading is done!");
        System.out.println("Clustering time: " + (double) (end - start) / 1000000000.0 + " seconds.");
        System.out.println("Max zoom level clusters # = " + this.advocatorClusters[maxZoom].size());
    }

    public void insert(Cluster c, int zoom) {
        double radius = getRadius(zoom);

        // Find all earlier advocators c can merge into
        KDTree<Advocator> advocatorsTree = this.advocatorsTrees[zoom];
        List<Advocator> advocators = advocatorsTree.within(c, radius);

        // if no group could be merged into, become a new Advocator itself
        if (advocators.isEmpty()) {
            Advocator newAdvocator = new Advocator(K);
            newAdvocator.seq = advocatorSeq ++;
            newAdvocator.cluster = c;
            newAdvocator.setDimensionValue(0, c.getDimensionValue(0));
            newAdvocator.setDimensionValue(1, c.getDimensionValue(1));
            advocatorsTree.insert(newAdvocator);
            c.expansionZoom = zoom + 1;
            c.numPoints = 0;
            this.advocatorClusters[zoom].add(c);
        }
        // if earlier advocators' groups could be merged into
        else {
            // find the earliest advocator
            Advocator earliestAdvocator = null;
            for (Advocator advocator: advocators) {
                if (earliestAdvocator == null) {
                    earliestAdvocator = advocator;
                }
                else if (earliestAdvocator.seq > advocator.seq) {
                    earliestAdvocator = advocator;
                }
            }

            // merge into earliest advocator's group
            Cluster cluster = earliestAdvocator.cluster;
            if (cluster.numPoints == 0) {
                cluster.numPoints = 1;
            }
            double wx = cluster.getDimensionValue(0) * cluster.numPoints;
            double wy = cluster.getDimensionValue(1) * cluster.numPoints;
            wx += c.getDimensionValue(0);
            wy += c.getDimensionValue(1);
            cluster.numPoints = cluster.numPoints + 1;
            cluster.setDimensionValue(0, wx / cluster.numPoints);
            cluster.setDimensionValue(1, wy / cluster.numPoints);
        }

        // insert this cluster into lower level
        if (zoom >= 1) {
            insert(createCluster(c.getDimensionValue(0), c.getDimensionValue(1), c.id, 0), zoom - 1);
        }
    }

    /**
     * Get an array of Clusters for given visible region and zoom level
     * note: search on advocators tree, but return the clusters attached to the advocators
     *
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param zoom
     * @return
     */
    public Cluster[] getClusters(double x0, double y0, double x1, double y1, int zoom) {
        double minLng = ((x0 + 180) % 360 + 360) % 360 - 180;
        double minLat = Math.max(-90, Math.min(90, y0));
        double maxLng = x1 == 180 ? 180 : ((x1 + 180) % 360 + 360) % 360 - 180;
        double maxLat = Math.max(-90, Math.min(90, y1));

        if (x1 - x0 >= 360) {
            minLng = -180;
            maxLng = 180;
        } else if (minLng > maxLng) {
            Cluster[] easternHem = this.getClusters(minLng, minLat, 180, maxLat, zoom);
            Cluster[] westernHem = this.getClusters(-180, minLat, maxLng, maxLat, zoom);
            return concat(easternHem, westernHem);
        }

        KDTree<Advocator> advocatorsTree = this.advocatorsTrees[this._limitZoom(zoom)];
        Cluster leftBottom = createPointCluster(minLng, maxLat);
        Cluster rightTop = createPointCluster(maxLng, minLat);
        List<Advocator> advocators = advocatorsTree.range(leftBottom, rightTop);
        Cluster[] clusters = new Cluster[advocators.size()];
        int i = 0;
        for (Advocator advocator: advocators) {
            Cluster cluster = advocator.cluster.clone();
            cluster.setDimensionValue(0, xLng(cluster.getDimensionValue(0)));
            cluster.setDimensionValue(1, yLat(cluster.getDimensionValue(1)));
            clusters[i] = cluster;
            i ++;
        }
        return clusters;
    }
}
