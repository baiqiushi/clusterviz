package model;

import java.util.ArrayList;
import java.util.List;

public class Cluster extends Point {
    public int zoom = Integer.MAX_VALUE;
    public int expansionZoom = Integer.MAX_VALUE;
    public int parentId = -1;
    public int numPoints = 0;
    // all data points belong to this cluster
    public List<Cluster> children = new ArrayList<>();

    public Cluster parent = null;
    public Advocator advocator = null;
    public Cluster advocatorCluster = null;

    public Cluster(int k) {
        super(k);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{id: " + id +
                ", numPoints: " + numPoints +
                ", zoom: " + zoom +
                ", expansionZoom: " + expansionZoom +
                ", parentId: " + parentId +
                ", coordinates: ");
        sb.append(super.toString());
        sb.append(", childrenSize: " + children.size());
        if (parent != null) {
            sb.append(", parent: " + parent.id);
        }
        if (advocator != null) {
            sb.append(", advocator: " + advocator.id);
        }
        if (advocatorCluster != null) {
            sb.append(", advocatorCluster: " + advocatorCluster.id);
        }
        sb.append("}");
        return sb.toString();
    }

    public Cluster clone() {
        Cluster to = new Cluster(this.k);
        to.setDimensionValue(0, this.getDimensionValue(0));
        to.setDimensionValue(1, this.getDimensionValue(1));
        to.id = this.id;
        to.numPoints = this.numPoints;
        to.children = this.children;
        to.parentId = this.parentId;
        to.zoom = this.zoom;
        to.expansionZoom = this.expansionZoom;
        return to;
    }
}
