package util;

public interface IKDPoint {

    int getId();

    void setId(int id);

    double getDimensionValue(int i);

    void setDimensionValue(int i, double value);

    boolean equalsTo(IKDPoint y);

    double distanceTo(IKDPoint y);

    boolean leftTo(IKDPoint y);

    boolean rightTo(IKDPoint y);
}
