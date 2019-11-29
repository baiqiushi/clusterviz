package util;

import java.util.List;

public interface I2DIndex {

    void insert(I2DPoint point);

    void load(I2DPoint[] points);

    void delete(I2DPoint point);

    List<I2DPoint> within(I2DPoint point, double radius);

    List<I2DPoint> range(I2DPoint leftBottom, I2DPoint rightTop);

    void print();
}
