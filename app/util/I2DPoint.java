package util;

public interface I2DPoint {
    int getId();

    void setId(int _id);

    double getX();

    double getY();

    void setX(double _x);

    void setY(double _y);

    boolean equalsTo(I2DPoint p2);

    double distanceTo(I2DPoint p2);

    boolean leftBelow(I2DPoint p2);

    boolean rightAbove(I2DPoint p2);

    I2DPoint clone();
}
