package clustering;

import model.Cluster;
import util.KDTree;

public class iSuperCluster extends SuperCluster {

    int DEFAULT_ARRAY_SIZE = 500;
    KDTree<Double>[] advocatorsTrees;

    public iSuperCluster() {
        this.trees = new KDTree[maxZoom + 1];
        this.clusters = new Cluster[maxZoom + 1][];
        this.advocatorsTrees = new KDTree[maxZoom];

        for (int i = 0; i < maxZoom + 1; i ++) {
            this.trees[i] = new KDTree<Double>(K);
            this.clusters[i] = new Cluster[DEFAULT_ARRAY_SIZE];
        }

        for (int i = 0; i < maxZoom; i ++) {
            this.advocatorsTrees[i] = new KDTree<Double>(K);
        }
    }

    public void load(Double[][] points) {
        this.totalNumberOfPoints += points.length;
        for (int i = 0; i < points.length; i ++) {
            insert(createPointCluster(points[i], i));
        }
    }

    protected void insert(Cluster c) {
        // TODO
    }
}
