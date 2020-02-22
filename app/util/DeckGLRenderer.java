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

    public int[][][] createRendering() {
        int[][][] rendering = new int[side][side][3];
        // init rendering with BG_COLOR
        for (int i = 0; i < side; i++) {
            for (int j = 0; j < side; j++) {
                rendering[i][j][0] = BG_COLOR[0]; // R
                rendering[i][j][1] = BG_COLOR[1]; // G
                rendering[i][j][2] = BG_COLOR[2]; // B
            }
        }
        return rendering;
    }

    /**
     *
     * @param rendering
     * @param _cX
     * @param _cY
     * @param _halfWidth
     * @param _halfHeight
     * @param point
     * @return boolean - if render the point on given rendering does not change the result, return false; else return true;
     */
    public boolean render(int[][][] rendering, double _cX, double _cY, double _halfWidth, double _halfHeight, Point point) {
        boolean isDifferent = false;
        double pixelWidth = 2 * _halfWidth;
        double pixelHeight = 2 * _halfHeight;
        double pixelDiagonal = Math.sqrt(Math.pow(pixelWidth, 2) + Math.pow(pixelHeight, 2));
        double px, py;
        double distanceToCenter;
        double inCircle;
        double alpha;
        int r, g, b;
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
                r = (int) (COLOR[0] * alpha + rendering[i][j][0] * (1.0 - alpha)); // R
                g = (int) (COLOR[1] * alpha + rendering[i][j][1] * (1.0 - alpha)); // G
                b = (int) (COLOR[2] * alpha + rendering[i][j][2] * (1.0 - alpha)); // B
                if (rendering[i][j][0] != r) {
                    isDifferent = true;
                    rendering[i][j][0] = r;
                }
                if (rendering[i][j][1] != g) {
                    isDifferent = true;
                    rendering[i][j][1] = g;
                }
                if (rendering[i][j][2] != b) {
                    isDifferent = true;
                    rendering[i][j][2] = b;
                }
            }
        }

        return isDifferent;
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
