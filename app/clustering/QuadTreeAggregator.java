package clustering;

import model.Cluster;
import model.Point;
import model.PointTuple;
import util.Constants;
import util.MyMemory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuadTreeAggregator extends SuperCluster {

    public static int QT_NODE_CAPACITY = 4;
    public static double highestResScale;

    public class BBox {
        public Point center;
        public double halfWidth;
        public double halfHeight;

        public BBox(Point center, double halfWidth, double halfHeight) {
            this.center = center;
            this.halfWidth = halfWidth;
            this.halfHeight = halfHeight;
        }

        public boolean containsPoint(Point point) {
            if (point.getX() >= (center.getX() - halfWidth)
                    && point.getY() >= (center.getY() - halfHeight)
                    && point.getX() <= (center.getX() + halfWidth)
                    && point.getY() <= (center.getY() + halfHeight)) {
                return true;
            }
            else {
                return false;
            }
        }

        public boolean intersectsBBox(BBox range) {
            // this bbox
            double left = this.center.getX() - this.halfWidth;
            double right = this.center.getX() + this.halfWidth;
            double bottom = this.center.getY() - this.halfHeight;
            double top = this.center.getY() + this.halfHeight;
            // range bbox
            double minX = range.center.getX() - range.halfWidth;
            double maxX = range.center.getX() + range.halfWidth;
            double minY = range.center.getY() - range.halfHeight;
            double maxY = range.center.getY() + range.halfHeight;

            // right to the right
            if (minX > right) return false;
            // left to the left
            if (maxX < left) return false;
            // above the top
            if (minY > top) return false;
            // below the bottom
            if (maxY < bottom) return false;

            return true;
        }

        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append("bbox[ (" + center.getX() + ", " + center.getY() + ")" + ", " + halfWidth + ", " + halfHeight + "]");
            return s.toString();
        }
    }

    public class QuadTree {
        // boundary of this node
        public BBox boundary;

        // points in this node
        public short pointsCount;
        // TODO - Currently intermediate notes also store QT_NODE_CAPACITY points
        public Point[] points;

        // children
        public QuadTree northWest;
        public QuadTree northEast;
        public QuadTree southWest;
        public QuadTree southEast;

        public QuadTree(BBox boundary) {
            this.boundary = boundary;
        }

        public boolean insert(Point point) {
            // Ignore objects that do not belong in this quad tree
            if (!this.boundary.containsPoint(point)) {
                return false;
            }
            // If there is space in this quad tree and if doesn't have subdivisions, add the object here
            if (this.pointsCount < QT_NODE_CAPACITY && this.northWest == null) {
                if (this.points == null) {
                   this.points = new Point[QT_NODE_CAPACITY];
                }
                this.points[this.pointsCount ++] = point;
                return true;
            }

            // if current node's boundary is smaller than highestResScale, drop this point
            if (Math.max(this.boundary.halfWidth, this.boundary.halfHeight) < highestResScale) {
                return false;
            }

            // Otherwise, subdivide
            if (this.northWest == null) {
                this.subdivide();
            }
            // then add the point to whichever node will accept it
            if (this.northWest.insert(point)) return true;
            if (this.northEast.insert(point)) return true;
            if (this.southWest.insert(point)) return true;
            if (this.southEast.insert(point)) return true;
            // Otherwise, the point cannot be inserted for some unknown reason (this should never happen)
            return false;
        }

        void subdivide() {
            // divide this node's boundary into 4 equal quadrants, and new children QuadTrees
            double x, y;
            double halfWidth, halfHeight;
            halfWidth = this.boundary.halfWidth / 2;
            halfHeight = this.boundary.halfHeight / 2;
            // northwest
            x = this.boundary.center.getX() - halfWidth;
            y = this.boundary.center.getY() + halfHeight;
            Point nwCenter = new Point(x, y, -1);
            BBox nwBoundary = new BBox(nwCenter, halfWidth, halfHeight);
            this.northWest = new QuadTree(nwBoundary);
            // northeast
            x = this.boundary.center.getX() + halfWidth;
            y = this.boundary.center.getY() + halfHeight;
            Point neCenter = new Point(x, y, -1);
            BBox neBoundary = new BBox(neCenter, halfWidth, halfHeight);
            this.northEast = new QuadTree(neBoundary);
            // southwest
            x = this.boundary.center.getX() - halfWidth;
            y = this.boundary.center.getY() - halfHeight;
            Point swCenter = new Point(x, y, -1);
            BBox swBoundary = new BBox(swCenter, halfWidth, halfHeight);
            this.southWest = new QuadTree(swBoundary);
            // southeast
            x = this.boundary.center.getX() + halfWidth;
            y = this.boundary.center.getY() - halfHeight;
            Point seCenter = new Point(x, y, -1);
            BBox seBoundary = new BBox(seCenter, halfWidth, halfHeight);
            this.southEast = new QuadTree(seBoundary);
        }

        public List<Point> range(BBox range, double resScale) {
            List<Point> pointsInRange = new ArrayList<>();

            // Automatically abort if the range does not intersect this quad
            if (!this.boundary.intersectsBBox(range))
                return pointsInRange; // empty list

            // Check points at this quad level
            if (this.points != null) {
                for (int i = 0; i < this.pointsCount; i ++) {
                    if (range.containsPoint(this.points[i]))
                        pointsInRange.add(this.points[i]);
                }
            }

            // Terminate here, if there are no children
            if (northWest == null)
                return pointsInRange;

            // TODO - when terminates due to resolution, those 4 points in this node might be overplotted
            // Terminate here, if this node's boundary is already smaller than resScale
            if (Math.max(this.boundary.halfWidth, this.boundary.halfHeight) < resScale)
                return pointsInRange;

            // Otherwise, add the points from the children
            pointsInRange.addAll(this.northWest.range(range, resScale));
            pointsInRange.addAll(this.northEast.range(range, resScale));
            pointsInRange.addAll(this.southWest.range(range, resScale));
            pointsInRange.addAll(this.southEast.range(range, resScale));

            return pointsInRange;
        }
    }

    QuadTree quadTree;

    //-Timing-//
    static final boolean keepTiming = true;
    Map<String, Double> timing;
    //-Timing-//

    public QuadTreeAggregator(int _minZoom, int _maxZoom, int resX, int resY) {
        this.minZoom = _minZoom;
        this.maxZoom = _maxZoom;

        double x = (Constants.MAX_X + Constants.MIN_X) / 2;
        double y = (Constants.MAX_Y + Constants.MIN_Y) / 2;
        double halfWidth = (Constants.MAX_X - Constants.MIN_X) / 2;
        double halfHeight = (Constants.MAX_Y - Constants.MIN_Y) / 2;
        Point center = new Point(x, y, -1);
        BBox boundary = new BBox(center, halfWidth, halfHeight);
        this.quadTree = new QuadTree(boundary);

        double pixelWidth = (Constants.MAX_X - Constants.MIN_X) / resX;
        double pixelHeight = (Constants.MAX_Y - Constants.MIN_Y) / resY;
        highestResScale = Math.min(pixelWidth / Math.pow(2, this.maxZoom - 4), pixelHeight / Math.pow(2, this.maxZoom - 4));

        // initialize the timing map
        if (keepTiming) {
            timing = new HashMap<>();
            timing.put("total", 0.0);
        }

        MyMemory.printMemory();
    }

    public void load(List<PointTuple> points) {
        System.out.println("QuadTree Aggregator loading " + points.size() + " points ... ...");
        long start = System.nanoTime();
        int count = 0;
        int skip = 0;
        for (PointTuple point: points) {
            if (this.quadTree.insert(createPoint(point.getX(), point.getY(), point.getId())))
                count ++;
            else
                skip ++;
        }
        this.totalNumberOfPoints += count;
        long end = System.nanoTime();
        System.out.println("QuadTree Aggregator inserted " + count + " points and skipped " + skip + " points.");
        if (keepTiming) timing.put("total", timing.get("total") + (double) (end - start) / 1000000000.0);
        System.out.println("QuadTree Aggregator loading is done!");
        System.out.println("Loading time: " + (double) (end - start) / 1000000000.0 + " seconds.");
        if (keepTiming) this.printTiming();

        MyMemory.printMemory();
    }

    private Point createPoint(double _x, double _y, int _id) {
        return new Point(lngX(_x), latY(_y), _id);
    }

    /**
     * Get an array of Clusters for given visible region and zoom level,
     *     then run tree-cut algorithm to choose a better subset of clusters to return
     *
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param zoom
     * @param treeCut
     * @param measure
     * @param pixels
     * @param bipartite
     * @param resX
     * @param resY
     * @return
     */
    public Cluster[] getClusters(double x0, double y0, double x1, double y1, int zoom, boolean treeCut, String measure, double pixels, boolean bipartite, int resX, int resY) {
        System.out.println("[QuadTree Aggregator] getting clusters for given range [" + x0 + ", " + y0 + "] ~ [" +
                x1 + ", " + y1 + "] and resolution [" + resX + " x " + resY + "]...");

        long start = System.nanoTime();
        double iX0 = lngX(x0);
        double iY0 = latY(y0);
        double iX1 = lngX(x1);
        double iY1 = latY(y1);
        double resScale = Math.min((iX1 - iX0) / resX, (iY0 - iY1) / resY);
        double x = (iX0 + iX1) / 2;
        double y = (iY0 + iY1) / 2;
        Point center = new Point(x, y, -1);
        double halfWidth = (iX1 - iX0) / 2;
        double halfHeight = (iY0 - iY1) / 2;
        BBox range = new BBox(center, halfWidth, halfHeight);

        System.out.println("[QuadTree Aggregator] starting range search on QuadTree with: \n" +
                "range = " + range + "; \n resScale = " + resScale + ";");

        List<Point> points = this.quadTree.range(range, resScale);
        List<Cluster> results = new ArrayList<>();
        for (Point point: points) {
            Cluster cluster = new Cluster(xLng(point.getX()), yLat(point.getY()), point.getId());
            results.add(cluster);
        }
        long end = System.nanoTime();
        double totalTime = (double) (end - start) / 1000000000.0;
        System.out.println("[QuadTree Aggregator] getClusters time: " + totalTime + " seconds.");
        return results.toArray(new Cluster[results.size()]);
    }

    private void printTiming() {
        System.out.println("[Total Time] " + timing.get("total") + " seconds.");
    }
}
