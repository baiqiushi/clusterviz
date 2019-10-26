package clustering;

import model.PointTuple;
import util.PostgreSQL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class SuperClusterTest {
    public static final int minZoom = 0;
    public static final int maxZoom = 17;

    public static void printPointTuples(PointTuple[] pointTuples, int limit) {
        System.out.println("==================== Point Tuples (" + pointTuples.length + ") ====================");
        for (int i = 0; i < Math.min(pointTuples.length, limit); i ++) {
            System.out.println(pointTuples[i]);
        }
        System.out.println("... ...");
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Please enter a keyword: (press ENTER to finish)");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String keyword = bufferedReader.readLine();

        // 0) Load original data from PostgreSQL
        PostgreSQL postgreSQL = new PostgreSQL();
        PointTuple[] pointTuples = postgreSQL.queryPointTuplesForKeyword(keyword);
        int length = pointTuples.length;
        for (int i = 0; i < length; i ++) {
            pointTuples[i].id = i;
        }

        printPointTuples(pointTuples, 10);

        double[][] points = new double[length][2];
        for (int i = 0; i < length; i ++) {
            points[i][0] = pointTuples[i].getDimensionValue(0);
            points[i][1] = pointTuples[i].getDimensionValue(1);
        }

        SuperCluster superCluster =  new SuperCluster(minZoom, maxZoom);
        superCluster.load(points);

        int zoom = 17;
        int[] labels = superCluster.getClusteringLabels(zoom);

        System.out.println("Super Cluster labels for zoom [" + zoom + "], size = " + labels.length);
        System.out.println(Arrays.toString(labels));
    }
}
