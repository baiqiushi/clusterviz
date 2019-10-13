package util;

import java.util.ArrayList;
import java.util.List;

public class MyTimer {
    public static List<Double> progressTimer = new ArrayList<>();
    private static long startTime = 0;
    private static long endTime = 0;

    public static void startTimer() {
        startTime = System.nanoTime();
    }

    public static void stopTimer() {
        endTime = System.nanoTime();
    }

    public static double durationSeconds() {
        return (double) (endTime - startTime) / 1000000000.0;
    }
}
