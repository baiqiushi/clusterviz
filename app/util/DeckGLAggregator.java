package util;

import model.Point;

import java.util.List;

public class DeckGLAggregator implements IAggregator {

    static double SMOOTH_EDGE_RADIUS = 0.5;
    static int[] COLOR = {0, 0, 255}; // blue
    static int[] BG_COLOR = {255, 255, 255}; // white

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
        int[][][] rendering = new int[side][side][3];
        // init rendering with BG_COLOR
        for (int i = 0; i < side; i ++) {
            for (int j = 0; j < side; j ++) {
                rendering[i][j][0] = BG_COLOR[0]; // R
                rendering[i][j][1] = BG_COLOR[1]; // G
                rendering[i][j][2] = BG_COLOR[2]; // B
            }
        }
        double pixelWidth = 2 * _halfWidth;
        double pixelHeight = 2 * _halfHeight;
        double pixelDiagonal = Math.sqrt(Math.pow(pixelWidth, 2) + Math.pow(pixelHeight, 2));
        double px, py;
        double distanceToCenter;
        double inCircle;
        double alpha;
        int[] newColor = new int[3];
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
                    // apply blend function DST_COLOR = SRC_COLOR * SRC_ALPHA + DST_COLOR * (1 - SRC_ALPHA)
                    rendering[i][j][0] = (int) (COLOR[0] * alpha + rendering[i][j][0] * (1.0 - alpha)); // R
                    rendering[i][j][1] = (int) (COLOR[1] * alpha + rendering[i][j][1] * (1.0 - alpha)); // G
                    rendering[i][j][2] = (int) (COLOR[2] * alpha + rendering[i][j][2] * (1.0 - alpha)); // B
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
                // apply blend function DST_COLOR = SRC_COLOR * SRC_ALPHA + DST_COLOR * (1 - SRC_ALPHA)
                newColor[0] = (int) (COLOR[0] * alpha + rendering[i][j][0] * (1.0 - alpha)); // R
                newColor[1] = (int) (COLOR[1] * alpha + rendering[i][j][1] * (1.0 - alpha)); // G
                newColor[2] = (int) (COLOR[2] * alpha + rendering[i][j][2] * (1.0 - alpha)); // B
                // as long as one pixel color is different, return false;
                if (rendering[i][j][0] != newColor[0] ||
                        rendering[i][j][1] != newColor[1] ||
                        rendering[i][j][2] != newColor[2]) {
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
