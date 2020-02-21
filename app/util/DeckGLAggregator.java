package util;

import model.Point;

import java.util.List;

public class DeckGLAggregator implements IAggregator {

    static double SMOOTH_EDGE_RADIUS = 0.5;
    static int[] COLOR = {0, 0, 255, 255};

    int radiusInPixels;
    int side;

    public DeckGLAggregator(int radiusInPixels) {
        this.radiusInPixels = radiusInPixels;
        this.side = radiusInPixels * 2 + 1;
        System.out.println("[DeckGLAggregator] initializing with { radiusInPixels: " + radiusInPixels + ", side: " + side + "}.");
    }

    @Override
    public boolean contains(double _cX, double _cY,
                            double _halfWidth, double _halfHeight,
                            List<Point> set1, Point point) {
        double[][] alphas = new double[side][side];
        double pixelWidth = 2 * _halfWidth;
        double pixelHeight = 2 * _halfHeight;
        double pixelDiagonal = Math.sqrt(Math.pow(pixelWidth, 2) + Math.pow(pixelHeight, 2));
        double px, py;
        double distanceToCenter;
        double inCircle;
        double alpha;
        // compute aggregated rendering from set1
        for (Point p: set1) {
            for (int i = 0; i < side; i ++) {
                for (int j = 0; j < side; j ++) {
                    px = _cX - radiusInPixels * pixelWidth + i * pixelWidth;
                    py = _cY - radiusInPixels * pixelHeight + j * pixelHeight;
                    // TODO - here we use pixel diagonal length
                    // current pixel to center point's distance in units of # of pixels
                    distanceToCenter = (Math.sqrt(Math.pow(px - p.getX(), 2) + Math.pow(py - p.getY(), 2))) / pixelDiagonal;
                    // how dense this pixel color
                    inCircle = smoothEdge(distanceToCenter, radiusInPixels);
                    alpha = 1.0 * inCircle;
                    alphas[i][j] = Math.max(alphas[i][j], alpha);
                }
            }
        }

        // compute rendering from only the point
        for (int i = 0; i < side; i ++) {
            for (int j = 0; j < side; j ++) {
                px = _cX - radiusInPixels * pixelWidth + i * pixelWidth;
                py = _cY - radiusInPixels * pixelHeight + j * pixelHeight;
                // TODO - here we use pixel diagonal length
                // current pixel to center point's distance in units of # of pixels
                distanceToCenter = (Math.sqrt(Math.pow(px - point.getX(), 2) + Math.pow(py - point.getY(), 2))) / pixelDiagonal;
                // how dense this pixel color
                inCircle = smoothEdge(distanceToCenter, radiusInPixels);
                alpha = 1.0 * inCircle;
                // as long as one pixel color can be denser, return false;
                if (alphas[i][j] < alpha) {
                    return false;
                }
            }
        }

        return true;
    }

    private double smoothEdge(double edge, double x) {
        return smoothStep(edge - SMOOTH_EDGE_RADIUS, edge + SMOOTH_EDGE_RADIUS, x);
    }

    private double smoothStep(double edge0, double edge1, double x) {
        if (x <= edge0) return 0.0;
        if (x >= edge1) return 1.0;
        double t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3 - 2 * t );
    }

    private double clamp(double x, double minVal, double maxVal) {
        return Math.min(Math.max(x, minVal), maxVal);
    }
}
