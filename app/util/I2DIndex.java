package util;

import java.util.List;

public interface I2DIndex<PointType> {

    void insert(PointType point);

    void load(PointType[] points);

    void delete(PointType point);

    List<PointType> within(PointType center, double radius);

    List<PointType> range(PointType leftBottom, PointType rightTop);

    void print();
}
