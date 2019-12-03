package util;

import model.Point;

public class MemoryTest {
    public static void main(String[] args) {
        I2DIndex<Point>[] gridIndexes = new I2DIndex[36];
        for (int i = 0; i < gridIndexes.length; i ++) {
            gridIndexes[i] = new GridIndex<>(2.384185791015625E-6);
        }

        MyMemory.printMemory();
    }
}
