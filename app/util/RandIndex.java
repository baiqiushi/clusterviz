package util;

public class RandIndex {
    public static double randIndex(int[] aLabels, int[] bLabels) {
        smile.validation.RandIndex randIndex = new smile.validation.RandIndex();
        return randIndex.measure(aLabels, bLabels);
    }

    public static double adjustedRandIndex(int[] aLabels, int[] bLabels) {
        smile.validation.AdjustedRandIndex adjustedRandIndex = new smile.validation.AdjustedRandIndex();
        return adjustedRandIndex.measure(aLabels, bLabels);
    }
}
