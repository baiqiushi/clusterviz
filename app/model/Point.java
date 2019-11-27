package model;


import util.IKDPoint;

public class Point implements IKDPoint {
    int k;
    double[] values;
    public int id;

    public Point(int k) {
        this.k = k;
        this.values = new double[k];
    }

    public double getDimensionValue(int i) {
        return values[i];
    }

    public void setDimensionValue(int i, double value) {
        this.values[i] = value;
    }

    public boolean equalsTo(IKDPoint y) {
        Point py = (Point) y;
        boolean equal = true;
        for (int i = 0; i < this.k; i ++) {
            if (this.values[i] != py.getDimensionValue(i)) {
                equal = false;
                break;
            }
        }
        return equal;
    }

    public double distanceTo(IKDPoint y) {
        double distance = 0.0;
        Point py = (Point) y;
        for (int i = 0; i < this.k; i ++) {
            distance += Math.pow(this.values[i] - py.getDimensionValue(i), 2);
        }
        distance = Math.sqrt(distance);
        return distance;
    }


    public boolean leftTo(IKDPoint y) {
        Point py = (Point) y;
        boolean left = true;
        for (int i = 0; i < this.k; i ++) {
            if (this.values[i] >= py.getDimensionValue(i)) {
                left = false;
                break;
            }
        }
        return left;
    }

    public boolean rightTo(IKDPoint y) {
        Point py = (Point) y;
        boolean right = true;
        for (int i = 0; i < this.k; i ++) {
            if (this.values[i] <= py.getDimensionValue(i)) {
                right = false;
                break;
            }
        }
        return right;
    }

    @Override
    public IKDPoint clone() {
        IKDPoint copy = new Point(k);
        copy.setId(id);
        for (int i = 0; i < k; i ++) {
            copy.setDimensionValue(i, this.getDimensionValue(i));
        }
        return copy;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < k; i ++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(values[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
