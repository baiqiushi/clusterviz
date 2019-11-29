package util;

public interface I2DPoint {
    int getId();

    void setId(int id);

    double getX();

    double getY();

    void setX(double x);

    void setY(double y);

    boolean equalsTo(I2DPoint p2);

    double distanceTo(I2DPoint p2);

    boolean leftTo(I2DPoint p2);

    boolean rightTo(I2DPoint p2);

    I2DPoint clone();
}
