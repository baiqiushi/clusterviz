package model;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

public class PointTuple extends Point {
    public static SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd hh:mm:ss");

    public long tid; // tuple id of point in database, different from point.id
    public Date timestamp;

    public PointTuple(int k) {
        super(k);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{timestamp: " + ft.format(timestamp) + ", coordinates: ");
        sb.append(super.toString());
        sb.append("}");
        return sb.toString();
    }

    // left bottom < right top
    public static Comparator getSpatialComparator() {
        return new Comparator<PointTuple> () {
            @Override
            public int compare(PointTuple p1, PointTuple p2) {
                if (p1.getDimensionValue(0) == p2.getDimensionValue(0)
                        && p1.getDimensionValue(1) == p2.getDimensionValue(1)) {
                    return 0;
                } else if (p1.getDimensionValue(0) == p2.getDimensionValue(0)) {
                    if (p1.getDimensionValue(1) < p2.getDimensionValue(1)) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else if (p1.getDimensionValue(1) == p2.getDimensionValue(1)) {
                    if (p1.getDimensionValue(0) < p2.getDimensionValue(0)) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else {
                    if (p1.getDimensionValue(0) < p2.getDimensionValue(0)) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            }
        };
    }

    // left bottom > right top
    public static Comparator getReverseSpatialComparator() {
        return new Comparator<PointTuple> () {
            @Override
            public int compare(PointTuple p1, PointTuple p2) {
                if (p1.getDimensionValue(0) == p2.getDimensionValue(0)
                        && p1.getDimensionValue(1) == p2.getDimensionValue(1)) {
                    return 0;
                } else if (p1.getDimensionValue(0) == p2.getDimensionValue(0)) {
                    if (p1.getDimensionValue(1) < p2.getDimensionValue(1)) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else if (p1.getDimensionValue(1) == p2.getDimensionValue(1)) {
                    if (p1.getDimensionValue(0) < p2.getDimensionValue(0)) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else {
                    if (p1.getDimensionValue(0) < p2.getDimensionValue(0)) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            }
        };
    }
}
