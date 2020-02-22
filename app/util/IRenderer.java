package util;

import model.Point;

public interface IRenderer {

    int[][][] createRendering();

    boolean render(int[][][] background, double _cX, double _cY, double _halfWidth, double _halfHeight, Point point);
}
