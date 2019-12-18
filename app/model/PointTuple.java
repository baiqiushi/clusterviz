package model;

import java.util.Comparator;

public class PointTuple extends Point {

    public long tid; // tuple id of point in database, different from point.id


    public PointTuple() {
        super();
    }

    // left bottom < right top
    public static Comparator getSpatialComparator() {
        return new Comparator<PointTuple> () {
            @Override
            public int compare(PointTuple p1, PointTuple p2) {
                if (p1.getX() == p2.getX()
                        && p1.getY() == p2.getY()) {
                    return 0;
                } else if (p1.getX() == p2.getX()) {
                    if (p1.getY() < p2.getY()) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else if (p1.getY() == p2.getY()) {
                    if (p1.getX() < p2.getX()) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else {
                    if (p1.getX() < p2.getX()) {
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
                if (p1.getX() == p2.getX()
                        && p1.getY() == p2.getY()) {
                    return 0;
                } else if (p1.getX() == p2.getX()) {
                    if (p1.getY() < p2.getY()) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else if (p1.getY() == p2.getY()) {
                    if (p1.getX() < p2.getX()) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else {
                    if (p1.getX() < p2.getX()) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            }
        };
    }
}
