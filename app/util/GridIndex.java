package util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public class GridIndex implements I2DIndex {

    class Grid {
        List<I2DPoint> points;

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

    private Grid[] grids;

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
        grids = new Grid[m * n];
    }

    /**
     * find grid position i on X axis
     *
     * @param x
     * @return
     */
    private int locateX(double x) {
        return (int) Math.floor((x - minX) / step);
    }

    /**
     * find grid position j on Y axis
     *
     * @param y
     * @return
     */
    private int locateY(double y) {
        return (int) Math.floor((y - minY) / step);
    }

    /**
     * find 1D array index of 2D space grid [i, j]
     *
     * @param i
     * @param j
     * @return
     */
    private int indexOf(int i, int j) {
        if (i < 0) i = 0;
        if (i > m - 1) i = m - 1;
        if (j < 0) j = 0;
        if (j > n - 1) j = n - 1;
        return j * m + i;
    }

    @Override
    public void insert(I2DPoint point) {
        // find the grid position [i, j]
        int i = locateX(point.getX());
        int j = locateY(point.getY());

        // find the index of the grid in grids array
        int index = indexOf(i, j);
        Grid grid = grids[index];

        // insert point into this grid's list
        grid.points.add(point);
    }

    @Override
    public void load(I2DPoint[] points) {
        for (I2DPoint p: points) {
            insert(p);
        }
    }

    @Override
    public void delete(I2DPoint point) {
        // find the grid position [i, j]
        int i = locateX(point.getX());
        int j = locateY(point.getY());

        // find the index of the grid in grids array
        int index = indexOf(i, j);
        Grid grid = grids[index];

        // remove point from this grid's list
        grid.points.remove(point);
    }

    /**
     * Assume radius = step,
     *     just find the four vertices of the circumscribed square of given circle,
     *     traverse the points in the 4 grids the vertices locate in respectively,
     *     compare the distance of each point with center,
     *     add a point p to result list if dist(p, center) <= radius.
     *
     * @param center
     * @param radius
     * @return
     */
    @Override
    public List<I2DPoint> within(I2DPoint center, double radius) {
        List<I2DPoint> result = new ArrayList<>();

        // find the left bottom right top of the circumscribed square of the circle
        double left = center.getX() - radius;
        double bottom = center.getY() - radius;
        double right = center.getX() + radius;
        double top = center.getY() + radius;

        // find the four grids that the four vertices of the circumscribed square locate in respectively
        int i0 = (int) Math.floor((left - minX) / step);
        int j0 = (int) Math.floor((bottom - minY) / step);
        // handle case when i1 and i0 are on edges of a grid, make sure they indicate the same grid
        int i1 = (int) Math.floor((right - minX) / step) - (right - minX) % step == 0.0? 1: 0;
        // handle case when j1 and j0 are on edges of a grid, make sure they indicate the same grid
        int j1 = (int) Math.floor((top - minY) / step) - (top - minY) % step == 0.0? 1: 0;

        // find all distinct indexes of grids
        Set<Integer> indexes = new HashSet<>();
        indexes.add(indexOf(i0, j0));
        indexes.add(indexOf(i0, j1));
        indexes.add(indexOf(i1, j0));
        indexes.add(indexOf(i1, j1));

        // traverse the points in grids, and add points within radius to center to the result list
        for (int index: indexes) {
            Grid grid = grids[index];
            for (I2DPoint p: grid.points) {
                if (p.distanceTo(center) <= radius) {
                    result.add(p);
                }
            }
        }

        return result;
    }

    @Override
    public List<I2DPoint> range(I2DPoint leftBottom, I2DPoint rightTop) {
        List<I2DPoint> result = new ArrayList<>();

        // find the offset of left bottom right top 4 vertices of the rectangle
        double left = (leftBottom.getX() - minX) / step;
        double bottom = (leftBottom.getY() - minY) / step;
        double right = (rightTop.getX() - minX) / step;
        double top = (rightTop.getY() - minY) / step;

        // find those inside grids and add their points directly into result list
        for (int i = (int) Math.ceil(left); i <= (int) Math.floor(right); i ++) {
            for (int j = (int) Math.ceil(bottom); j <= (int) Math.floor(top); j ++) {
                int index = indexOf(i, j);
                result.addAll(grids[index].points);
            }
        }

        // traverse points in those edge grids and add points that within the rectangle range
        // TODO

        return null;
    }

    @Override
    public void print() {

    }
}
