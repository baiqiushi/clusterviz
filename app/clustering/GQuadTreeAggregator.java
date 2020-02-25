package clustering;

import javafx.util.Pair;
import model.Cluster;
import model.Point;
import model.PointTuple;
import util.Constants;
import util.DeckGLRenderer;
import util.IRenderer;
import util.MyMemory;

import java.util.*;

public class GQuadTreeAggregator extends SuperCluster {

    public static double highestResScale;

    public static IRenderer aggregator;

    public class QuadTree {
        // Store count of the sub-tree
        public int count;
        public int[][][] rendering;
        public List<Point> samples;

        // children
        public QuadTree northWest;
        public QuadTree northEast;
        public QuadTree southWest;
        public QuadTree southEast;

        public QuadTree() {
            this.count = 0;
        }

        public boolean containsPoint(double cX, double cY, double halfWidth, double halfHeight, Point point) {
            if (point.getX() >= (cX - halfWidth)
                    && point.getY() >= (cY - halfHeight)
                    && point.getX() < (cX + halfWidth)
                    && point.getY() < (cY + halfHeight)) {
                return true;
            }
            else {
                return false;
            }
        }

        public boolean intersectsBBox(double c1X, double c1Y, double halfWidth1, double halfHeight1,
                                      double c2X, double c2Y, double halfWidth2, double halfHeight2) {
            // bbox 1
            double left = c1X - halfWidth1;
            double right = c1X + halfWidth1;
            double bottom = c1Y - halfHeight1;
            double top = c1Y + halfHeight1;
            // bbox 2
            double minX = c2X - halfWidth2;
            double maxX = c2X + halfWidth2;
            double minY = c2Y - halfHeight2;
            double maxY = c2Y + halfHeight2;

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

        public boolean insert(double cX, double cY, double halfWidth, double halfHeight, Point point, IRenderer aggregator, int level) {
            // Ignore objects that do not belong in this quad tree
            if (!containsPoint(cX, cY, halfWidth, halfHeight, point)) {
                return false;
            }
            // If this node is leaf and empty, put this point on this node
            if (this.samples == null && this.northWest == null) {
                this.samples = new ArrayList<>();
                this.samples.add(point);
                this.rendering = aggregator.createRendering();
                aggregator.render(this.rendering, cX, cY, halfWidth, halfHeight, point);
                this.count = 1;
                return true;
            }
            // Else, add count into this node
            this.count ++;

            // if boundary is smaller than highestResScale, drop this point
            if (Math.max(halfWidth, halfHeight) * 2 < highestResScale) {
                this.samples.add(point);
                return true;
            }

            // Otherwise, subdivide
            if (this.northWest == null) {
                this.subdivide();
                // insert current node's point into corresponding quadrant
                this.insertNorthWest(cX, cY, halfWidth, halfHeight, this.samples.get(0), aggregator, level + 1);
                this.insertNorthEast(cX, cY, halfWidth, halfHeight, this.samples.get(0), aggregator, level + 1);
                this.insertSouthWest(cX, cY, halfWidth, halfHeight, this.samples.get(0), aggregator, level + 1);
                this.insertSouthEast(cX, cY, halfWidth, halfHeight, this.samples.get(0), aggregator, level + 1);
            }

            // update the rendering of this node
            boolean isDifferent = aggregator.render(this.rendering, cX, cY, halfWidth, halfHeight, point);
            // if new rendering is different, store this point within samples
            // (only start storing samples from level 10)
            if (level > 10 && isDifferent) this.samples.add(point);

            // insert new point into corresponding quadrant
            if (insertNorthWest(cX, cY, halfWidth, halfHeight, point, aggregator, level + 1)) return true;
            if (insertNorthEast(cX, cY, halfWidth, halfHeight, point, aggregator, level + 1)) return true;
            if (insertSouthWest(cX, cY, halfWidth, halfHeight, point, aggregator, level + 1)) return true;
            if (insertSouthEast(cX, cY, halfWidth, halfHeight, point, aggregator, level + 1)) return true;

            return false;
        }

        boolean insertNorthWest(double _cX, double _cY, double _halfWidth, double _halfHeight, Point point, IRenderer aggregator, int level) {
            double halfWidth = _halfWidth / 2;
            double halfHeight = _halfHeight / 2;
            double cX = _cX - halfWidth;
            double cY = _cY + halfHeight;
            return this.northWest.insert(cX, cY, halfWidth, halfHeight, point, aggregator, level);
        }

        boolean insertNorthEast(double _cX, double _cY, double _halfWidth, double _halfHeight, Point point, IRenderer aggregator, int level) {
            double halfWidth = _halfWidth / 2;
            double halfHeight = _halfHeight / 2;
            double cX = _cX + halfWidth;
            double cY = _cY + halfHeight;
            return this.northEast.insert(cX, cY, halfWidth, halfHeight, point, aggregator, level);
        }

        boolean insertSouthWest(double _cX, double _cY, double _halfWidth, double _halfHeight, Point point, IRenderer aggregator, int level) {
            double halfWidth = _halfWidth / 2;
            double halfHeight = _halfHeight / 2;
            double cX = _cX - halfWidth;
            double cY = _cY - halfHeight;
            return this.southWest.insert(cX, cY, halfWidth, halfHeight, point, aggregator, level);
        }

        boolean insertSouthEast(double _cX, double _cY, double _halfWidth, double _halfHeight, Point point, IRenderer aggregator, int level) {
            double halfWidth = _halfWidth / 2;
            double halfHeight = _halfHeight / 2;
            double cX = _cX + halfWidth;
            double cY = _cY - halfHeight;
            return this.southEast.insert(cX, cY, halfWidth, halfHeight, point, aggregator, level);
        }

        void subdivide() {
            this.northWest = new QuadTree();
            this.northEast = new QuadTree();
            this.southWest = new QuadTree();
            this.southEast = new QuadTree();
            nodesCount += 4;
        }

        public List<Point> range(double ncX, double ncY, double nhalfWidth, double nhalfHeight,
                                 double rcX, double rcY, double rhalfWidth, double rhalfHeight,
                                 double resScale, int level) {
            List<Point> pointsInRange = new ArrayList<>();

            // Automatically abort if the range does not intersect this quad
            if (!intersectsBBox(ncX, ncY, nhalfWidth, nhalfHeight, rcX, rcY, rhalfWidth, rhalfHeight))
                return pointsInRange; // empty list

            // Terminate here, if there are no children
            if (this.northWest == null) {
                highestLevelForQuery = Math.max(highestLevelForQuery, level);
                if (this.samples != null) {
                    pointsInRange.addAll(this.samples);
                }
                return pointsInRange;
            }

            // Terminate here, if this node's boundary is already smaller than resScale
            if (Math.max(nhalfWidth, nhalfHeight) * 2 <= resScale) {
                lowestLevelForQuery = Math.min(lowestLevelForQuery, level);
                // add this node's samples
                pointsInRange.addAll(this.samples);
                return pointsInRange;
            }

            // Otherwise, add the points from the children
            double cX, cY;
            double halfWidth, halfHeight;
            halfWidth = nhalfWidth / 2;
            halfHeight = nhalfHeight / 2;
            // northwest
            cX = ncX - halfWidth;
            cY = ncY + halfHeight;
            pointsInRange.addAll(this.northWest.range(cX, cY, halfWidth, halfHeight,
                    rcX, rcY, rhalfWidth, rhalfHeight, resScale, level + 1));

            // northeast
            cX = ncX + halfWidth;
            cY = ncY + halfHeight;
            pointsInRange.addAll(this.northEast.range(cX, cY, halfWidth, halfHeight,
                    rcX, rcY, rhalfWidth, rhalfHeight, resScale, level + 1));

            // southwest
            cX = ncX - halfWidth;
            cY = ncY - halfHeight;
            pointsInRange.addAll(this.southWest.range(cX, cY, halfWidth, halfHeight,
                    rcX, rcY, rhalfWidth, rhalfHeight, resScale, level + 1));

            // southeast
            cX = ncX + halfWidth;
            cY = ncY - halfHeight;
            pointsInRange.addAll(this.southEast.range(cX, cY, halfWidth, halfHeight,
                    rcX, rcY, rhalfWidth, rhalfHeight, resScale, level + 1));

            return pointsInRange;
        }

        public void print() {
            System.out.println("=================== GQuadTree ===================");
            Queue<Pair<Integer, QuadTree>> queue = new LinkedList<>();
            queue.add(new Pair<>(0, this));
            int currentLevel = -1;
            while (queue.size() > 0) {
                Pair<Integer, QuadTree> currentEntry = queue.poll();
                int level = currentEntry.getKey();
                QuadTree currentNode = currentEntry.getValue();
                if (level > currentLevel) {
                    System.out.println();
                    System.out.print("[" + level + "] ");
                    currentLevel = level;
                }
                System.out.print(currentNode.samples == null? "0": currentNode.samples.size());
                System.out.print(", ");
                if (currentNode.northWest != null) {
                    queue.add(new Pair<>(level + 1, currentNode.northWest));
                }
                if (currentNode.northEast != null) {
                    queue.add(new Pair<>(level + 1, currentNode.northEast));
                }
                if (currentNode.southWest != null) {
                    queue.add(new Pair<>(level + 1, currentNode.southWest));
                }
                if (currentNode.southEast != null) {
                    queue.add(new Pair<>(level + 1, currentNode.southEast));
                }
            }
            System.out.println();
        }

        public void statistics() {
            System.out.println("=================== GQuadTree Statistics ===================");
            System.out.println("level,    # samples,    # nodes,    # samples/node,    min # samples,    max # samples");
            Queue<Pair<Integer, QuadTree>> queue = new LinkedList<>();
            queue.add(new Pair<>(0, this));
            int currentLevel = -1;
            int totalNumberOfSamples = 0;
            int totalNumberOfNodes = 0;
            int totalMinNumberOfSamples = Integer.MAX_VALUE;
            int totalMaxNumberOfSamples = 0;
            int numberOfSamples = 0;
            int numberOfNodes = 0;
            int minNumberOfSamples = Integer.MAX_VALUE;
            int maxNumberOfSamples = 0;
            while (queue.size() > 0) {
                Pair<Integer, QuadTree> currentEntry = queue.poll();
                int level = currentEntry.getKey();
                QuadTree currentNode = currentEntry.getValue();
                int currentNumberOfSamples = currentNode.samples == null? 0: currentNode.samples.size();
                numberOfSamples += currentNumberOfSamples;
                numberOfNodes += 1;
                minNumberOfSamples = Math.min(currentNumberOfSamples, minNumberOfSamples);
                maxNumberOfSamples = Math.max(currentNumberOfSamples, maxNumberOfSamples);
                if (level > currentLevel) {
                    System.out.println(level + ",    " + numberOfSamples + ",    " + numberOfNodes + ",    " + (numberOfSamples/numberOfNodes) + ",    " + minNumberOfSamples + ",    " + maxNumberOfSamples);
                    currentLevel = level;
                    totalNumberOfSamples += numberOfSamples;
                    totalNumberOfNodes += numberOfNodes;
                    totalMinNumberOfSamples = Math.min(totalMinNumberOfSamples, minNumberOfSamples);
                    totalMaxNumberOfSamples = Math.max(totalMaxNumberOfSamples, maxNumberOfSamples);
                    numberOfSamples = 0;
                    numberOfNodes = 0;
                    minNumberOfSamples = Integer.MAX_VALUE;
                    maxNumberOfSamples = 0;
                }
                if (currentNode.northWest != null) {
                    queue.add(new Pair<>(level + 1, currentNode.northWest));
                }
                if (currentNode.northEast != null) {
                    queue.add(new Pair<>(level + 1, currentNode.northEast));
                }
                if (currentNode.southWest != null) {
                    queue.add(new Pair<>(level + 1, currentNode.southWest));
                }
                if (currentNode.southEast != null) {
                    queue.add(new Pair<>(level + 1, currentNode.southEast));
                }
            }
            System.out.println("-------------------------- Summary -------------------------");
            System.out.println("total # samples,    total # nodes,    total # samples/node,    total min # samples,    total max # samples");
            System.out.println(totalNumberOfSamples + ",    " + totalNumberOfNodes + ",    " + (totalNumberOfSamples/totalNumberOfNodes) + ",    " + totalMinNumberOfSamples + " ,   " + totalMaxNumberOfSamples);
        }

        public void histograms(int someLevel) {
            int[] histogramForSamplesOnIntermediateNodes = new int[101]; // 0 ~ 99, >=100
            int[] histogramForRawPointsOnLeafNodes = new int[101]; // 0 ~ 99, >=100
            int[] histogramForSamplesOnIntermediateNodesAtLevel = new int[101]; // 0 ~ 99, >=100
            int[] histogramForRawPointsOnIntermediateNodesAtLevel = new int[101]; // 0 ~ 999, >=1000

            Queue<Pair<Integer, QuadTree>> queue = new LinkedList<>();
            queue.add(new Pair<>(0, this));
            while (queue.size() > 0) {
                Pair<Integer, QuadTree> currentEntry = queue.poll();
                int level = currentEntry.getKey();
                QuadTree currentNode = currentEntry.getValue();
                int currentNumberOfSamples = currentNode.samples == null? 0: currentNode.samples.size();
                if (currentNode.northWest != null) {
                    queue.add(new Pair<>(level + 1, currentNode.northWest));
                }
                if (currentNode.northEast != null) {
                    queue.add(new Pair<>(level + 1, currentNode.northEast));
                }
                if (currentNode.southWest != null) {
                    queue.add(new Pair<>(level + 1, currentNode.southWest));
                }
                if (currentNode.southEast != null) {
                    queue.add(new Pair<>(level + 1, currentNode.southEast));
                }
                if (currentNode.northWest != null && level > 10){
                    if (currentNumberOfSamples > 99) histogramForSamplesOnIntermediateNodes[100] += 1;
                    else histogramForSamplesOnIntermediateNodes[currentNumberOfSamples] += 1;
                    if (level == someLevel) {
                        if (currentNumberOfSamples > 99) histogramForSamplesOnIntermediateNodesAtLevel[100] += 1;
                        else histogramForSamplesOnIntermediateNodesAtLevel[currentNumberOfSamples] += 1;
                        if (currentNode.count > 990) histogramForRawPointsOnIntermediateNodesAtLevel[100] += 1;
                        else histogramForRawPointsOnIntermediateNodesAtLevel[currentNode.count/10] += 1;
                    }
                }
                else if (currentNode.northWest == null) {
                    if (currentNumberOfSamples > 99) histogramForRawPointsOnLeafNodes[100] += 1;
                    else histogramForRawPointsOnLeafNodes[currentNumberOfSamples] += 1;
                }
            }

            System.out.println("=================== GQuadTree Histogram for Samples on Intermediate Nodes ===================");
            System.out.println("# of samples on node,    # of nodes");
            for (int i = 0; i < 100; i ++) {
                System.out.println(i + ",    " + histogramForSamplesOnIntermediateNodes[i]);
            }
            System.out.println(">=100,    " + histogramForSamplesOnIntermediateNodes[100]);

            System.out.println("=================== GQuadTree Histogram for Raw Points on Leaf Nodes ===================");
            System.out.println("# of raw points on node,    # of nodes");
            for (int i = 0; i < 100; i ++) {
                System.out.println(i + ",    " + histogramForRawPointsOnLeafNodes[i]);
            }
            System.out.println(">=100,    " + histogramForRawPointsOnLeafNodes[100]);

            System.out.println("=================== GQuadTree Histogram for Samples on Intermediate Nodes at level " + someLevel + " ===================");
            System.out.println("# of samples on node,    # of nodes");
            for (int i = 0; i < 100; i ++) {
                System.out.println(i + ",    " + histogramForSamplesOnIntermediateNodesAtLevel[i]);
            }
            System.out.println(">=100,    " + histogramForSamplesOnIntermediateNodesAtLevel[100]);

            System.out.println("=================== GQuadTree Histogram for Raw Points on Intermediate Nodes at level " + someLevel + " ===================");
            System.out.println("# of raw points on node,    # of nodes");
            for (int i = 0; i < 100; i ++) {
                System.out.println((0 + i*10) + "~" + (9 + i*10) + ",    " + histogramForRawPointsOnIntermediateNodesAtLevel[i]);
            }
            System.out.println(">=1000,    " + histogramForRawPointsOnIntermediateNodesAtLevel[100]);
        }
    }

    QuadTree quadTree;
    double quadTreeCX;
    double quadTreeCY;
    double quadTreeHalfWidth;
    double quadTreeHalfHeight;
    int totalStoredNumberOfPoints = 0;
    static long nodesCount = 0; // count quad-tree nodes
    static int lowestLevelForQuery = Integer.MAX_VALUE; // the lowest level of range searching for a query
    static int highestLevelForQuery = 0; // the highest level of range searching for a query

    //-Timing-//
    static final boolean keepTiming = true;
    Map<String, Double> timing;
    //-Timing-//

    public GQuadTreeAggregator(int _minZoom, int _maxZoom, int resX, int resY) {
        this.minZoom = _minZoom;
        this.maxZoom = _maxZoom;

        this.quadTreeCX = (Constants.MAX_X + Constants.MIN_X) / 2;
        this.quadTreeCY = (Constants.MAX_Y + Constants.MIN_Y) / 2;
        this.quadTreeHalfWidth = (Constants.MAX_X - Constants.MIN_X) / 2;
        this.quadTreeHalfHeight = (Constants.MAX_Y - Constants.MIN_Y) / 2;
        this.quadTree = new QuadTree();

        double pixelWidth = (Constants.MAX_X - Constants.MIN_X) / resX;
        double pixelHeight = (Constants.MAX_Y - Constants.MIN_Y) / resY;
        highestResScale = Math.min(pixelWidth / Math.pow(2, this.maxZoom - 4), pixelHeight / Math.pow(2, this.maxZoom - 4));

        aggregator = new DeckGLRenderer(Constants.RADIUS_IN_PIXELS);

        // initialize the timing map
        if (keepTiming) {
            timing = new HashMap<>();
            timing.put("total", 0.0);
        }

        MyMemory.printMemory();
    }

    public void load(List<PointTuple> points) {
        System.out.println("General QuadTree Aggregator loading " + points.size() + " points ... ...");
        long start = System.nanoTime();
        this.totalNumberOfPoints += points.size();
        int count = 0;
        int skip = 0;
        for (PointTuple point: points) {
            if (this.quadTree.insert(this.quadTreeCX, this.quadTreeCY, this.quadTreeHalfWidth, this.quadTreeHalfHeight,
                    createPoint(point.getX(), point.getY(), point.getId()), aggregator, 0))
                count ++;
            else
                skip ++;
        }
        this.totalStoredNumberOfPoints += count;
        long end = System.nanoTime();
        System.out.println("General QuadTree Aggregator inserted " + count + " points and skipped " + skip + " points.");
        if (keepTiming) timing.put("total", timing.get("total") + (double) (end - start) / 1000000000.0);
        System.out.println("General QuadTree Aggregator loading is done!");
        System.out.println("Loading time: " + (double) (end - start) / 1000000000.0 + " seconds.");
        if (keepTiming) this.printTiming();

        MyMemory.printMemory();

        //-DEBUG-//
        System.out.println("==== Until now ====");
        System.out.println("General QuadTree has processed " + this.totalNumberOfPoints + " points.");
        System.out.println("General QuadTree has stored " + this.totalStoredNumberOfPoints + " points.");
        System.out.println("General QuadTree has skipped " + skip + " points.");
        System.out.println("General QuadTree has generated " + nodesCount + " nodes.");
        this.quadTree.statistics();
        this.quadTree.histograms(12);
        //-DEBUG-//
    }

    protected Point createPoint(double _x, double _y, int _id) {
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
        System.out.println("[General QuadTree Aggregator] getting clusters for given range [" + x0 + ", " + y0 + "] ~ [" +
                x1 + ", " + y1 + "] and resolution [" + resX + " x " + resY + "]...");

        long start = System.nanoTime();
        double iX0 = lngX(x0);
        double iY0 = latY(y0);
        double iX1 = lngX(x1);
        double iY1 = latY(y1);
        double resScale = Math.min((iX1 - iX0) / resX, (iY0 - iY1) / resY);
        double rcX = (iX0 + iX1) / 2;
        double rcY = (iY0 + iY1) / 2;
        double rhalfWidth = (iX1 - iX0) / 2;
        double rhalfHeight = (iY0 - iY1) / 2;

        if (treeCut) {
            resScale = resScale * pixels;
        }

        System.out.println("[General QuadTree Aggregator] starting range search on QuadTree with: \n" +
                "range = [(" + rcX + ", " + rcY + "), " + rhalfWidth + ", " + rhalfHeight + "] ; \n" +
                "resScale = " + resScale + ";");

        lowestLevelForQuery = Integer.MAX_VALUE;
        highestLevelForQuery = 0;

        List<Point> points = this.quadTree.range(this.quadTreeCX, this.quadTreeCY, this.quadTreeHalfWidth, this.quadTreeHalfHeight,
                rcX, rcY, rhalfWidth, rhalfHeight, resScale, 0);
        List<Cluster> results = new ArrayList<>();
        for (Point point: points) {
            Cluster cluster = new Cluster(xLng(point.getX()), yLat(point.getY()), point.getId());
            results.add(cluster);
        }
        long end = System.nanoTime();
        double totalTime = (double) (end - start) / 1000000000.0;
        System.out.println("[General QuadTree Aggregator] getClusters time: " + totalTime + " seconds.");
        System.out.println("[General QuadTree Aggregator] lowest level for this query: " + lowestLevelForQuery);
        System.out.println("[General QuadTree Aggregator] highest level for this query: " + highestLevelForQuery);
        return results.toArray(new Cluster[results.size()]);
    }

    private void printTiming() {
        System.out.println("[Total Time] " + timing.get("total") + " seconds.");
    }
}
