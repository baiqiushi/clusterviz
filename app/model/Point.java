package model;

import util.I2DPoint;

public class Point implements I2DPoint {
    double x;
    double y;
    int id;

    public Point() {
        x = 0.0;
        y = 0.0;
        id = -1;
    }

    public Point(double _x, double _y, int _id) {
        x = _x;
        y = _y;
        id = _id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double _x) {
        x = _x;
    }

    public void setY(double _y) {
        y = _y;
    }

    public boolean equalsTo(I2DPoint p2) {
        if (x == p2.getX() && y == p2.getY()) return true;
        else return false;
    }

    public double distanceTo(I2DPoint p2) {
        return Math.sqrt(Math.pow(x - p2.getX(), 2) + Math.pow(y - p2.getY(), 2));
    }


    public boolean leftBelow(I2DPoint p2) {
        if (x < p2.getX() && y < p2.getY()) return true;
        else return false;
    }

    public boolean rightAbove(I2DPoint p2) {
        if (x > p2.getX() && y > p2.getY()) return true;
        else return false;
    }

    public I2DPoint clone() {
        I2DPoint copy = new Point(x, y, id);
        return copy;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(id);
        sb.append(":");
        sb.append("(");
        sb.append(x);
        sb.append(",");
        sb.append(y);
        sb.append(")");
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
