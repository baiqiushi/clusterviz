package clustering;

import model.Cluster;
import model.Advocator;
import util.KDTree;

import java.util.ArrayList;
import java.util.List;

public class iSuperCluster extends SuperCluster {

    List<Cluster> maxZoomClusters;
    KDTree<Advocator> advocatorsTree;
    int pointIdSeq;
    int advocatorSeq;

    public iSuperCluster() {
        this.trees = new KDTree[maxZoom + 1];
        this.clusters = new Cluster[maxZoom][];
        this.maxZoomClusters = new ArrayList<>();
        this.advocatorsTree = new KDTree<>(K);
        this.pointIdSeq = 0;
        this.advocatorSeq = 0;
    }

    public iSuperCluster(int _minZoom, int _maxZoom) {
        this.minZoom = _minZoom;
        this.maxZoom = _maxZoom;
        this.trees = new KDTree[maxZoom + 1];
        this.clusters = new Cluster[maxZoom][];
        this.maxZoomClusters = new ArrayList<>();
        this.advocatorsTree = new KDTree<>(K);
        this.pointIdSeq = 0;
        this.advocatorSeq = 0;
    }

    public void load(double[][] points) {
        System.out.println("incremental SuperCluster loading " + points.length + " points ... ...");
        long start = System.nanoTime();

        this.totalNumberOfPoints += points.length;

        // merge all points into the maxZoom level clusters
        for (int i = 0; i < points.length; i ++) {
            mergePoint(createPointCluster(points[i], this.pointIdSeq ++));
        }

        // then re-cluster all the levels above maxZoom level
        Cluster[] clusters = this.maxZoomClusters.toArray(new Cluster[this.maxZoomClusters.size()]);
        for (int i = 0; i < clusters.length; i ++) {
            clusters[i].zoom = Integer.MAX_VALUE;
        }
        this.trees[maxZoom] = new KDTree<>(K);
        this.trees[maxZoom].load(clusters);

        // cluster the clusters on maxZoom level to form maxZoom-1 level, etc.;
        for (int z = maxZoom - 1; z >= minZoom; z --) {
            // create a new set of clusters for the zoom and index them with a KD-tree
            clusters = this._clusters(clusters, z);

            this.trees[z] = new KDTree<>(K);
            this.trees[z].load(clusters);
            this.clusters[z] = clusters;
        }

        long end = System.nanoTime();
        System.out.println("incremental SuperCluster loading is done!");
        System.out.println("Takes time: " + (double) (end - start) / 1000000000.0 + " seconds.");
        System.out.println("Max zoom level clusters # = " + this.maxZoomClusters.size());
    }

    private void mergePoint(Cluster c) {
        double radius = getRadius(maxZoom);
        // Find all earlier advocators c can merge into
        List<Advocator> advocators = advocatorsTree.within(c, radius);

        // if no group could be merged into, become a new Advocator itself
        if (advocators.isEmpty()) {
            Advocator newAdvocator = new Advocator(K);
            newAdvocator.seq = advocatorSeq ++;
            newAdvocator.cluster = c;
            newAdvocator.setDimensionValue(0, c.getDimensionValue(0));
            newAdvocator.setDimensionValue(1, c.getDimensionValue(1));
            advocatorsTree.insert(newAdvocator);
            c.id = (c.id << 5) + (maxZoom + 1);
            c.expansionZoom = maxZoom + 1;
            c.numPoints = 0;
            this.maxZoomClusters.add(c);
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
            // do not store the children for the maxZoom level clusters,
            // otherwise, the points instances memory can not be released.
        }
    }
}
