package util;

public class IndexCreator {
    public static I2DIndex createIndex(String indexType, double step) {
        switch (indexType) {
            case "GridIndex":
                return new GridIndex<>(step);
            case "KDTree":
                return new KDTree<>();
        }
        return new KDTree<>();
    }

    public static I2DIndex[] createIndexArray(String indexType, int size) {
        switch (indexType) {
            case "GridIndex":
                return new GridIndex[size];
            case "KDTree":
                return new KDTree[size];
        }
        return new KDTree[size];
    }
}
