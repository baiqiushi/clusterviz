package model;

import util.Flags;

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
    // its advocator cluster in one of its children
    public Cluster advocatorCluster = null;
    // flag is true when this cluster has been deleted from its level's tree
    public boolean dirty = false;

    // flag to indicate its status from lower level children's shifting
    public Flags flag = Flags.NONE;
    // only valid when flag == Flags.UPDATED, update type for this cluster
    public Flags update = Flags.NONE; // MERGE / SPLIT
    // only valid when update == Flags.MERGE/Flags.SPLIT, delta cluster for update
    public ClusterDelta updateDelta = null;
    // sequence number of this cluster to indicate its processing order
    public int seq = 0;

    // max pairwise distance of all its children centroids
    public double diameter = -1.0;

    public Cluster(double _x, double _y, int _id) {
        super(_x, _y, _id);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{id: " + id +
                ", numPoints: " + numPoints +
                ", zoom: " + zoom +
                ", expansionZoom: " + expansionZoom +
                ", parentId: " + parentId +
                ", coordinates: ");
        sb.append("(");
        sb.append(x);
        sb.append(",");
        sb.append(y);
        sb.append(")");
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
        Cluster to = new Cluster(this.x, this.y, this.id);
        // copy values
        to.numPoints = this.numPoints;
        to.parentId = this.parentId;
        to.zoom = this.zoom;
        to.expansionZoom = this.expansionZoom;
        // copy handles only
        to.children = this.children;
        to.parent = this.parent;
        to.advocator = this.advocator;
        to.advocatorCluster = this.advocatorCluster;
        return to;
    }
}
