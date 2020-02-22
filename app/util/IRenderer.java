package util;

import model.Point;

public interface IRenderer {
    int[][][] render(int[][][] background, double _cX, double _cY, double _halfWidth, double _halfHeight, Point point);

    boolean isDifferent(int[][][] rendering1, int[][][] rendering2);
}
