package util;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Grid Index for I2DPoint
 *
 *     Parameters:
 *         left-bottom [minX, minY],
 *         right-top [maxX, maxY],
 *         step length
 *
 *     Algorithms:
 *         (1) number of grids at X axis:
 *                m = ceil((maxX - minX) / step);
 *         (2) number of grids at y axis:
 *                n = ceil((maxY - minY) / step);
 *         (3) a point (x, y) is mapped to a grid [i, j] in 2D space:
 *                i = floor((x - minX) / step);
 *                j = floor((y - minY) / step);
 *         (4) a grid [i, j] in 2D space is mapped to an element in array grids:
 *                index = j * m + i;
 *
 */
public class GridIndex<PointType extends I2DPoint> implements I2DIndex<PointType> {

    class Grid {
        List<PointType> points;

        public Grid() {
            this.points = new ArrayList<>();
        }
    }

    private double minX;
    private double minY;
    private double maxX;
    private double maxY;
    private double step;
    private int m;
    private int n;
    private int size = 0;

    private Grid[][] grids;

    public GridIndex(double left, double bottom, double right, double top, double step) {
        this.minX = left;
        this.minY = bottom;
        this.maxX = right;
        this.maxY = top;
        this.step = step;

        // calculate number of grids
        m = (int) Math.ceil((maxX - minX) / step);
        n = (int) Math.ceil((maxY - minY) / step);

        // initialize an array of grids
        grids = (Grid[][]) Array.newInstance(Grid.class, m, n);

        for (int i = 0; i < m; i ++) {
            for (int j = 0; j < n; j ++) {
                grids[i][j] = new Grid();
            }
        }
    }

    /**
     * find grid position i on X axis
     *
     * @param x
     * @return
     */
    private int locateX(double x) {
        int i = (int) Math.floor((x - minX) / step);
        i = i < 0? 0: i;
        i = i > m-1? m-1: i;
        return i;
    }

    /**
     * find grid position j on Y axis
     *
     * @param y
     * @return
     */
    private int locateY(double y) {
        int j = (int) Math.floor((y - minY) / step);
        j = j < 0? 0: j;
        j = j > n-1? n-1: j;
        return j;
    }

    @Override
    public void insert(PointType point) {
        size ++;
        // find the grid position [i, j]
        int i = locateX(point.getX());
        int j = locateY(point.getY());
        Grid grid = grids[i][j];

        // insert point into this grid's list
        grid.points.add(point);
    }

    @Override
    public void load(PointType[] points) {
        for (PointType p: points) {
            insert(p);
        }
    }

    @Override
    public void delete(PointType point) {
        // find the grid position [i, j]
        int i = locateX(point.getX());
        int j = locateY(point.getY());
        Grid grid = grids[i][j];

        // remove point from this grid's list
        if (grid.points.remove(point)) size --;
    }

    /**
     * find the four vertices of the inscribed square of given circle,
     *     all of grids inside inscribed square are in result directly;
     * find the four vertices of the circumscribed square of given circle,
     *     traverse the grids between circumscribed square edges and inscribed edges;
     *
     * @param center
     * @param radius
     * @return
     */
    @Override
    public List<PointType> within(I2DPoint center, double radius) {
        List<PointType> result = new ArrayList<>();

        // find the left bottom right top of the inscribed square of the circle
        double inLeftX = center.getX() - (radius / Math.sqrt(2.0));
        double inBottomY = center.getY() - (radius / Math.sqrt(2.0));
        double inRightX = center.getX() + (radius / Math.sqrt(2.0));
        double inTopY = center.getY() + (radius / Math.sqrt(2.0));

        // find the offset of left bottom right top 4 vertices of the inscribed square
        double inLeft = (inLeftX - minX) / step;
        double inBottom = (inBottomY - minY) / step;
        double inRight = (inRightX - minX) / step;
        double inTop = (inTopY - minY) / step;

        // bound with domain
        inLeft = inLeft < 0.0? 0.0: inLeft;
        inBottom = inBottom < 0.0? 0.0: inBottom;
        inRight = inRight > m? m: inRight;
        inTop = inTop > n? n: inTop;

        // find those inside grids and add their points directly into result list
        for (int i = (int) Math.ceil(inLeft); i < (int) Math.floor(inRight); i ++) {
            for (int j = (int) Math.ceil(inBottom); j < (int) Math.floor(inTop); j ++) {
                result.addAll(grids[i][j].points);
            }
        }

        // find the left bottom right top of the circumscribed square of the circle
        double outLeftX = center.getX() - radius;
        double outBottomY = center.getY() - radius;
        double outRightX = center.getX() + radius;
        double outTopY = center.getY() + radius;

        // find the offset of left bottom right top 4 vertices of the circumscribed square
        double outLeft = (outLeftX - minX) / step;
        double outBottom = (outBottomY - minY) / step;
        double outRight = (outRightX - minX) / step;
        double outTop = (outTopY - minY) / step;

        // bound within domain
        outLeft = outLeft < 0.0? 0.0: outLeft;
        outBottom = outBottom < 0.0? 0.0: outBottom;
        outRight = outRight > m? m: outRight;
        outTop = outTop > n? n: outTop;

        // traverse the rectangle between outLeft and inLeft
        // note: if inLeft % 1 == 0.0,
        //       it means inLeft aligns with grid edge,
        //       and the right grids have been traversed above, so skip that column here.
        for (int i = (int) Math.floor(outLeft); i <= (int) Math.floor(inLeft) - (inLeft % 1 == 0.0? 1: 0); i ++) {
            // note: if outTop % 1 == 0.0, outTop aligns with grid edge, then do not check grids above outTop
            for (int j = (int) Math.floor(outBottom); j <= (int) Math.floor(outTop) - (outTop % 1 == 0.0? 1: 0); j ++) {
                Grid grid = grids[i][j];
                for (PointType p: grid.points) {
                    if (p.distanceTo(center) <= radius) {
                        result.add(p);
                    }
                }
            }
        }

        // traverse the rectangle between inRight and outRight
        // note: if outRight % 1 == 0.0,
        //       it means outRight aligns with grid edge, then do not check the grids right after outRight.
        for (int i = (int) Math.floor(inRight); i <= (int) Math.floor(outRight) - (outRight % 1 == 0.0? 1: 0); i ++) {
            for (int j = (int) Math.floor(outBottom); j <= (int) Math.floor(outTop) - (outTop % 1 == 0.0? 1: 0); j ++) {
                Grid grid = grids[i][j];
                for (PointType p: grid.points) {
                    if (p.distanceTo(center) <= radius) {
                        result.add(p);
                    }
                }
            }
        }

        // traverse the rectangle between outBottom and inBottom
        // note: if inBottom % 1 == 0.0,
        //       it means inBottom aligns with grid edge,
        //       and the grids above inBottom have been traversed above, so skip that row here.
        for (int j = (int) Math.floor(outBottom); j <= (int) Math.floor(inBottom) - (inBottom % 1 == 0.0? 1: 0); j ++) {
            // note: skip duplicated traversal (left-bottom, right-bottom)
            for (int i = (int) Math.floor(inLeft) + 1; i <= (int) Math.floor(inRight) - 1; i ++) {
                Grid grid = grids[i][j];
                for (PointType p: grid.points) {
                    if (p.distanceTo(center) <= radius) {
                        result.add(p);
                    }
                }
            }
        }

        // traverse the rectangle between inTop and outTop
        // note: if outTop % 1 == 0.0,
        //       it means outTop aligns with grid edge, then do not check the grids above outTop.
        for (int j = (int) Math.floor(inTop); j <= (int) Math.floor(outTop) - (outTop % 1 == 0.0? 1: 0); j ++) {
            // note: skip duplicated traversal (left-top, right-top)
            for (int i = (int) Math.floor(inLeft) + 1; i <= (int) Math.floor(inRight) - 1; i ++) {
                Grid grid = grids[i][j];
                for (PointType p: grid.points) {
                    if (p.distanceTo(center) <= radius) {
                        result.add(p);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public List<PointType> range(I2DPoint leftBottom, I2DPoint rightTop) {
        List<PointType> result = new ArrayList<>();

        // find the offset of left bottom right top 4 vertices of the rectangle
        double left = (leftBottom.getX() - minX) / step;
        double bottom = (leftBottom.getY() - minY) / step;
        double right = (rightTop.getX() - minX) / step;
        double top = (rightTop.getY() - minY) / step;

        // bound the rectangle within domain
        left = left < 0.0? 0.0: left;
        bottom = bottom < 0.0? 0.0: bottom;
        right = right > m? m: right;
        top = top > n? n: top;

        // find those inside grids and add their points directly into result list
        for (int i = (int) Math.ceil(left); i < (int) Math.floor(right); i ++) {
            for (int j = (int) Math.ceil(bottom); j < (int) Math.floor(top); j ++) {
                result.addAll(grids[i][j].points);
            }
        }

        /** traverse points in those edge grids and add points that within the rectangle range */
        // left edge does not align with edge of grids
        if (left % 1 != 0.0) {
            int i = (int) Math.floor(left);
            // traverse left edge grids,
            // note: if top % 1 == 0.0, top aligns with grid edge, then do not check grids above top
            for (int j = (int) Math.floor(bottom); j <= (int) Math.floor(top) - (top % 1 == 0.0? 1: 0); j ++) {
                Grid grid = grids[i][j];
                for (PointType p: grid.points) {
                    if (p.rightAbove(leftBottom) && p.leftBelow(rightTop)) {
                        result.add(p);
                    }
                }
            }
        }

        // right edge does not align with edge of grids, and not the same with left
        if (right % 1 != 0.0 && Math.floor(left) < Math.floor(right)) {
            int i = (int) Math.floor(right);
            // traverse right edge grids,
            // note: if top % 1 == 0.0, top aligns with grid edge, then do not check grids above top
            for (int j = (int) Math.floor(bottom); j <= (int) Math.floor(top) - (top % 1 == 0.0? 1: 0); j ++) {
                Grid grid = grids[i][j];
                for (PointType p: grid.points) {
                    if (p.rightAbove(leftBottom) && p.leftBelow(rightTop)) {
                        result.add(p);
                    }
                }
            }
        }

        // bottom edge does not align with edge of grids
        if (bottom % 1 != 0.0) {
            int j = (int) Math.floor(bottom);
            // traverse bottom edge grids,
            // note: skip duplicated traversal (left-bottom, right-bottom)
            for (int i = (int) Math.floor(left) + 1; i <= (int) Math.floor(right) - 1; i ++) {
                Grid grid = grids[i][j];
                for (PointType p: grid.points) {
                    if (p.rightAbove(leftBottom) && p.leftBelow(rightTop)) {
                        result.add(p);
                    }
                }
            }
        }

        // top edge does not align with edge of grids, and not the same with bottom
        if (top % 1 != 0.0 && Math.floor(bottom) < Math.floor(top)) {
            int j = (int) Math.floor(top);
            // traverse top edge grids,
            // note: skip duplicated traversal (left-top, right-top)
            for (int i = (int) Math.floor(left) + 1; i < (int) Math.floor(right) - 1; i ++) {
                Grid grid = grids[i][j];
                for (PointType p: grid.points) {
                    if (p.rightAbove(leftBottom) && p.leftBelow(rightTop)) {
                        result.add(p);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void print() {
        System.out.println("=================== GridIndex ===================");
        System.out.println("size = " + size);
        for (int j = n - 1; j >= 0; j --) {
            for (int i = 0; i < m; i ++) {
                Grid grid = grids[i][j];
                if (grid != null) {
                    System.out.print(grid.points.size());
                } else {
                    System.out.print("  ");
                }
                System.out.print(" | ");
            }
            System.out.println();
        }
    }

    public int size() {
        return this.size;
    }
}
