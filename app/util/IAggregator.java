package util;

import model.Point;
import java.util.List;

public interface IAggregator {
    public boolean contains(double _cX, double _cY,
                            double _halfWidth, double _halfHeight,
                            List<Point> set1, Point point);
}
