package util;

public class IndexCreator {
    public static I2DIndex createIndex(String indexType, double step) {
        switch (indexType) {
            case "GridIndex":
                System.out.println("[IndexCreator] is creating GridIndex with step = " + step + " ...");
                return new GridIndex<>(step);
            case "KDTree":
                System.out.println("[IndexCreator] is creating KDTree ...");
                return new KDTree<>();
        }
        return new KDTree<>();
    }

    public static I2DIndex[] createIndexArray(String indexType, int size) {
        switch (indexType) {
            case "GridIndex":
                System.out.println("[IndexCreator] is creating GridIndex[" + size + "] array ...");
                return new GridIndex[size];
            case "KDTree":
                System.out.println("[IndexCreator] is creating KDTree[" + size + "] array ...");
                return new KDTree[size];
        }
        return new KDTree[size];
    }
}
