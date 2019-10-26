package clustering;

import model.Cluster;
import model.PointTuple;
import util.PostgreSQL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class iSuperClusterTest {
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

        iSuperCluster isuperCluster =  new iSuperCluster(minZoom, maxZoom);

        int zoom = 17;

        // simulate batches of data coming into iSuperCluster
        int total = points.length;
        int batchCount = 10;
        int batchSize = total / batchCount;
        for (int i = 0; i < batchCount; i ++) {
            // slice points array to a new array for this batch
            int start = i * batchSize;
            int end = Math.min(i * batchSize + batchSize - 1, total);
            double[][] batchPoints = new double[end - start + 1][2];
            int k = 0;
            for (int j = start; j <= end; j++) {
                batchPoints[k][0] = points[j][0];
                batchPoints[k][1] = points[j][1];
                k++;
            }

            isuperCluster.load(batchPoints);

            int[] labels = isuperCluster.getClusteringLabels(zoom);
            System.out.println("incremental Super Cluster labels for zoom [" + zoom + "], size = " + labels.length);
            System.out.println(Arrays.toString(labels));
        }
    }
}
