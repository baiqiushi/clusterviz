package exps;

import clustering.SuperCluster;
import model.Cluster;
import model.PointTuple;
import util.PostgreSQL;
import util.RandIndex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InsertionOrders {

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

        // 1) Load original data from PostgreSQL
        PostgreSQL postgreSQL = new PostgreSQL();
        PointTuple[] pointTuples = postgreSQL.queryPointTuplesForKeyword(keyword);
        int length = pointTuples.length;
        for (int i = 0; i < length; i ++) {
            pointTuples[i].id = i;
        }

        printPointTuples(pointTuples, 10);

        // 2) Run SuperCluster for original order (A)
        Double[][] aPoints = new Double[length][2];
        for (int i = 0; i < length; i ++) {
            aPoints[i][0] = pointTuples[i].getDimensionValue(0);
            aPoints[i][1] = pointTuples[i].getDimensionValue(1);
        }
        SuperCluster aSuperCluster =  new SuperCluster();
        aSuperCluster.load(aPoints);

        // 3) Run SuperCluster for reversed order (B)
        Double[][] bPoints = new Double[length][2];
        for (int i = 0; i < length; i ++) {
            bPoints[length - 1 - i][0] = pointTuples[i].getDimensionValue(0);
            bPoints[length - 1 - i][1] = pointTuples[i].getDimensionValue(1);
        }
        SuperCluster bSuperCluster = new SuperCluster();
        bSuperCluster.load(bPoints);

        // 4) Run SuperCluster for spatial (left-bottom to right-top) order (C)
        List<PointTuple> pointTuplesList = Arrays.asList(pointTuples);
        Collections.sort(pointTuplesList, PointTuple.getSpatialComparator());
        PointTuple[] cPointTuples = pointTuplesList.toArray(new PointTuple[pointTuplesList.size()]);
        Double[][] cPoints = new Double[length][2];
        for (int i = 0; i < cPointTuples.length; i ++) {
            cPoints[i][0] = cPointTuples[i].getDimensionValue(0);
            cPoints[i][1] = cPointTuples[i].getDimensionValue(1);
        }
        SuperCluster cSuperCluster = new SuperCluster();
        cSuperCluster.load(cPoints);

        // Output zoom level 2 clusters for all clustering results
        int exampleZoom = 2;
        Cluster[] clusters = aSuperCluster.getClusters(exampleZoom);
        System.out.println("================= " + exampleZoom + "th zoom level of A clusters (" + clusters.length + ") =================");
        for (int i = 0; i < clusters.length; i ++) {
            System.out.println(clusters[i]);
        }
        clusters = bSuperCluster.getClusters(exampleZoom);
        System.out.println("================= " + exampleZoom + "th zoom level of B clusters (" + clusters.length + ") =================");
        for (int i = 0; i < clusters.length; i ++) {
            System.out.println(clusters[i]);
        }
        clusters = cSuperCluster.getClusters(exampleZoom);
        System.out.println("================= " + exampleZoom + "th zoom level of C clusters (" + clusters.length + ") =================");
        for (int i = 0; i < clusters.length; i ++) {
            System.out.println(clusters[i]);
        }

        // 5) Compute the rand index of A vs B and A vs C
        System.out.println("==================== RandIndex of different insertion orders ====================");
        System.out.println("==    A - original order");
        System.out.println("==    B - reversed order");
        System.out.println("==    C - spatial (left-bottom to right-top) order");
        System.out.println("=================================================================================");
        System.out.println(keyword + ",    " + length);
        System.out.println("zoom,    ri(A-B),    ri(A-C)");
        for (int z = 1; z <= 10; z ++) {
            // labels for A
            int[] aLabels = aSuperCluster.getClusteringLabels(z);

            // labels for B
            int[] bLabels = bSuperCluster.getClusteringLabels(z);
            for (int i = 0; i < bLabels.length / 2; i ++) {
                int temp = bLabels[i];
                bLabels[i] = bLabels[bLabels.length - 1 - i];
                bLabels[bLabels.length - 1 - i] = temp;
            }

            // labels for C
            int[] cLabelsStage = cSuperCluster.getClusteringLabels(z);
            int[] cLabels = new int[cLabelsStage.length];
            for (int i = 0; i < cLabelsStage.length; i ++) {
                PointTuple labeledPointTuple = cPointTuples[i];
                cLabels[labeledPointTuple.id] = cLabelsStage[i];
            }

            System.out.println(z +
                    ",    " + RandIndex.randIndex(aLabels, bLabels) +
                    ",    " + RandIndex.randIndex(aLabels, cLabels));
        }


        postgreSQL.disconnectDB();
    }
}
