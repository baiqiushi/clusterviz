package model;

public class Advocator extends Point {

    public Cluster cluster;

    public Advocator(double _x, double _y, int _id) {
        super(_x, _y, _id);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("id: " + id);
        sb.append(" coordinates: " + super.toString());
        sb.append(" cluster: " + cluster);
        sb.append("}");
        return sb.toString();
    }

    public Advocator clone() {
        Advocator to = new Advocator(this.x, this.y, this.id);
        // copy handle only
        to.cluster = this.cluster;
        return to;
    }
}
