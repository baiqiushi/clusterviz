package model;


import util.IKDPoint;

public class Point implements IKDPoint<Double> {
    int k;
    Double[] values;

    public Point(int k) {
        this.k = k;
        this.values = new Double[k];
    }

    public void init(Double[] values) {
        for (int i = 0; i < this.k; i ++) {
            this.values[i] = values[i];
        }
    }

    public Double getDimensionValue(int i) {
        return values[i];
    }

    public void setDimensionValue(int i, Double value) {
        this.values[i] = value;
    }

    public boolean equalsTo(IKDPoint y) {
        Point py = (Point) y;
        boolean equal = true;
        for (int i = 0; i < this.k; i ++) {
            if (!this.values[i].equals(py.getDimensionValue(i))) {
                equal = false;
                break;
            }
        }
        return equal;
    }

    public Double distanceTo(IKDPoint y) {
        Double distance = 0.0;
        Point py = (Point) y;
        for (int i = 0; i < this.k; i ++) {
            distance += Math.pow(this.values[i] - py.getDimensionValue(i), 2);
        }
        distance = Math.sqrt(distance);
        return distance;
    }

    public void increaseSize() {
    }

    public boolean leftTo(IKDPoint y) {
        Point py = (Point) y;
        boolean left = true;
        for (int i = 0; i < this.k; i ++) {
            if (this.values[i].compareTo(py.getDimensionValue(i)) >= 0) {
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
            if (this.values[i].compareTo(py.getDimensionValue(i)) <= 0) {
                right = false;
                break;
            }
        }
        return right;
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
}
