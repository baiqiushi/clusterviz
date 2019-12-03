package util;

import model.Point;

public class MemoryTest {
    public static void main(String[] args) {
        I2DIndex<Point> gridIndex = new GridIndex<>(2.384185791015625E-6);
        long memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("Memory usage: " + memoryUsage / (1024.0*1024.0) + " MB.");
    }
}
