package model;

public class Advocator extends Point {

    public Cluster cluster;

    public Advocator(int k) {
        super(k);
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
        Advocator to = new Advocator(this.k);
        to.cluster = this.cluster;
        to.id = this.id;
        to.values[0] = this.values[0];
        to.values[1] = this.values[1];
        return to;
    }
}
