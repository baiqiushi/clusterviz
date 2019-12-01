package util;

import model.Point;

public class GridIndexTest {
    public static void main(String[] args) {
        I2DIndex<Point> gridIndex = new GridIndex<>(0, 0, 100, 100, 2);

        // put a point at every vertex of side-1 squares
        for (int i = 0; i <= 99; i ++) {
            for (int j = 0; j <= 99; j ++) {
                Point p = new Point(i, j, j*100+i);
                gridIndex.insert(p);
            }
        }

        // gridIndex.print();

        // circle with center (3,3) and radius 1.5,
        // should return 9 points: (2,2), (2,3), (2,4), (3,2), (3,3), (3,4), (4,2), (4,3), (4,4)
        System.out.println(gridIndex.within(new Point(3, 3, 0), 1.5));
    }
}
