package util;

import model.Point;

public class DeckGLRenderer implements IRenderer {

    static double SMOOTH_EDGE_RADIUS = 0.5;
    static int[] COLOR = {0, 0, 255}; // blue
    static int[] BG_COLOR = {255, 255, 255}; // white

    int radiusInPixels;
    int side;

    public DeckGLRenderer(int radiusInPixels) {
        this.radiusInPixels = radiusInPixels;
        this.side = radiusInPixels * 2 + 1;
        System.out.println("[DeckGLAggregator] initializing with { radiusInPixels: " + radiusInPixels + ", side: " + side + "}.");
    }

    public int[][][] render(int[][][] background, double _cX, double _cY, double _halfWidth, double _halfHeight, Point point) {
        // result rendering
        int[][][] rendering = new int[side][side][3];

        // if no background provided
        if (background == null) {
            // init rendering with BG_COLOR
            for (int i = 0; i < side; i++) {
                for (int j = 0; j < side; j++) {
                    rendering[i][j][0] = BG_COLOR[0]; // R
                    rendering[i][j][1] = BG_COLOR[1]; // G
                    rendering[i][j][2] = BG_COLOR[2]; // B
                }
            }
        }
        else {
            // init rendering with background
            for (int i = 0; i < side; i++) {
                for (int j = 0; j < side; j++) {
                    rendering[i][j][0] = background[i][j][0]; // R
                    rendering[i][j][1] = background[i][j][1]; // G
                    rendering[i][j][2] = background[i][j][2]; // B
                }
            }
        }
        double pixelWidth = 2 * _halfWidth;
        double pixelHeight = 2 * _halfHeight;
        double pixelDiagonal = Math.sqrt(Math.pow(pixelWidth, 2) + Math.pow(pixelHeight, 2));
        double px, py;
        double distanceToCenter;
        double inCircle;
        double alpha;

        // render the point on the background as a new rendering
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
                rendering[i][j][0] = (int) (COLOR[0] * alpha + rendering[i][j][0] * (1.0 - alpha)); // R
                rendering[i][j][1] = (int) (COLOR[1] * alpha + rendering[i][j][1] * (1.0 - alpha)); // G
                rendering[i][j][2] = (int) (COLOR[2] * alpha + rendering[i][j][2] * (1.0 - alpha)); // B
            }
        }

        return rendering;
    }

    public boolean isDifferent(int[][][] rendering1, int[][][] rendering2) {
        for (int i = 0; i < side; i ++) {
            for (int j = 0; j < side; j ++) {
                for (int k = 0; k < 3; k ++) {
                    if (rendering1[i][j][k] != rendering2[i][j][k]) return true;
                }
            }
        }
        return false;
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
