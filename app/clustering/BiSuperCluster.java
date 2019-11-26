package clustering;

import model.Advocator;
import model.Cluster;
import util.KDTree;
import util.MyLinkedList;
import util.MyTimer;

import java.util.*;

public class BiSuperCluster extends SuperCluster {
    // advocators at each level
    KDTree<Advocator>[] advocatorsTrees;
    // clusters at each level
    MyLinkedList<Cluster>[] advocatorClusters;
    // temporary tree for current shifting level of clusters
    KDTree<Cluster> clustersTree;
    int pointIdSeq;
    int advocatorSeq;
    int totalShiftCount = 0;

    double miu = 0.5;

    //-Timing-//
    static final boolean keepTiming = true;
    Map<String, Double> insertTiming = new HashMap<>();
    Map<String, Double> shiftTiming = new HashMap<>();
    //-Timing-//

    public BiSuperCluster(int _minZoom, int _maxZoom, boolean _analysis) {
        this.minZoom = _minZoom;
        this.maxZoom = _maxZoom;
        this.advocatorsTrees = new KDTree[maxZoom + 1];
        this.advocatorClusters = new MyLinkedList[maxZoom + 1];
        this.pointIdSeq = 0;
        this.advocatorSeq = 0;

        for (int i = 0; i <= maxZoom; i ++) {
            this.advocatorsTrees[i] = new KDTree<>(K);
            this.advocatorClusters[i] = new MyLinkedList<>();
        }
    }

    public void load(double[][] points) {
        System.out.println("Batch incremental SuperCluster loading " + points.length + " points ... ...");
        long start = System.nanoTime();

        this.totalNumberOfPoints += points.length;

        if (keepTiming) MyTimer.startTimer();
        // insert points to max zoom level one by one
        for (int i = 0; i < points.length; i ++) {
            insert(createPointCluster(points[i], this.pointIdSeq ++), maxZoom);
        }
        if (keepTiming) MyTimer.stopTimer();
        if (keepTiming) {
            if (insertTiming.containsKey("total")) {
                insertTiming.put("total", insertTiming.get("total") + MyTimer.durationSeconds());
            } else {
                insertTiming.put("total", MyTimer.durationSeconds());
            }
        }

        if (keepTiming) MyTimer.startTimer();
        int shiftCount = 0;
        // shift clusters with significant relocation bottom-up
        for (int z = maxZoom; z > minZoom; z --) {
            if (keepTiming) MyTimer.startTimer();
            // shifting at level z affects clustering results of level (z-1)
            double r = getRadius(z - 1);
            if (keepTiming) MyTimer.stopTimer();
            if (keepTiming) {
                if (shiftTiming.containsKey("getRadius")) {
                    shiftTiming.put("getRadius", shiftTiming.get("getRadius") + MyTimer.durationSeconds());
                } else {
                    shiftTiming.put("getRadius", MyTimer.durationSeconds());
                }
            }

            if (keepTiming) MyTimer.startTimer();
            // build KD-Tree for current level's clusters, to prepare for toMerge and toSplit functions
            // TODO - use a index structure that supports update operation for clusters
            this.clustersTree = new KDTree<>(K);
            // initialize this level's KD-Tree of clusters
            this.advocatorClusters[z].startLoop();
            Cluster x;
            while ((x = this.advocatorClusters[z].next()) != null) {
                this.clustersTree.insert(x);
            }
            System.out.println("this.clustersTree load advocatorClusters done...");
            if (keepTiming) MyTimer.stopTimer();
            if (keepTiming) {
                if (shiftTiming.containsKey("buildKDTree")) {
                    shiftTiming.put("buildKDTree", shiftTiming.get("buildKDTree") + MyTimer.durationSeconds());
                } else {
                    shiftTiming.put("buildKDTree", MyTimer.durationSeconds());
                }
            }

            if (keepTiming) MyTimer.startTimer();
            // for current level, shift clusters one by one
            int shiftCountLevel = 0;
            this.advocatorClusters[z].startLoop();
            Cluster cluster;
            while ((cluster = this.advocatorClusters[z].next()) != null) {
                Advocator advocator = cluster.advocator;
                if (cluster.distanceTo(advocator) > miu * r) {
                    shiftCountLevel ++;
                    shift(cluster, advocator, z);
                }
            }
            System.out.println("zoom level [" + z + "] shifted " + shiftCountLevel + " clusters.");
            shiftCount += shiftCountLevel;
            if (keepTiming) MyTimer.stopTimer();
            if (keepTiming) {
                if (shiftTiming.containsKey("shift")) {
                    shiftTiming.put("shift", shiftTiming.get("shift") + MyTimer.durationSeconds());
                } else {
                    shiftTiming.put("shift", MyTimer.durationSeconds());
                }
            }
        }
        this.totalShiftCount += shiftCount;
        if (keepTiming) MyTimer.stopTimer();
        if (keepTiming) {
            if (shiftTiming.containsKey("total")) {
                shiftTiming.put("total", shiftTiming.get("total") + MyTimer.durationSeconds());
            } else {
                shiftTiming.put("total", MyTimer.durationSeconds());
            }
        }

        long end = System.nanoTime();
        System.out.println("Batch incremental SuperCluster loading is done!");
        System.out.println("Clustering time: " + (double) (end - start) / 1000000000.0 + " seconds.");
        System.out.println("Max zoom level clusters # = " + this.advocatorClusters[maxZoom].size());
        System.out.println("This batch shift clusters: " + shiftCount);
        System.out.println("Total shift clusters: " + this.totalShiftCount);
        if (keepTiming) this.printTiming();
    }

    public void insert(Cluster c, int zoom) {
        if (keepTiming) MyTimer.startTimer();
        double radius = getRadius(zoom);
        if (keepTiming) MyTimer.stopTimer();
        if (keepTiming) {
            if (insertTiming.containsKey("getRadius")) {
                insertTiming.put("getRadius", insertTiming.get("getRadius") + MyTimer.durationSeconds());
            } else {
                insertTiming.put("getRadius", MyTimer.durationSeconds());
            }
        }

        if (keepTiming) MyTimer.startTimer();
        // Find all earlier advocators c can merge into
        KDTree<Advocator> advocatorsTree = this.advocatorsTrees[zoom];
        List<Advocator> advocators = advocatorsTree.within(c, radius);
        if (keepTiming) MyTimer.stopTimer();
        if (keepTiming) {
            if (insertTiming.containsKey("rangeSearch")) {
                insertTiming.put("rangeSearch", insertTiming.get("rangeSearch") + MyTimer.durationSeconds());
            } else {
                insertTiming.put("rangeSearch", MyTimer.durationSeconds());
            }
        }

        // if no group could be merged into, become a new Advocator itself
        if (advocators.isEmpty()) {
            if (keepTiming) MyTimer.startTimer();
            Advocator newAdvocator = new Advocator(K);
            newAdvocator.id = advocatorSeq ++;
            newAdvocator.cluster = c;
            c.advocator = newAdvocator;
            newAdvocator.setDimensionValue(0, c.getDimensionValue(0));
            newAdvocator.setDimensionValue(1, c.getDimensionValue(1));
            if (keepTiming) MyTimer.stopTimer();
            if (keepTiming) {
                if (insertTiming.containsKey("createAdvocator")) {
                    insertTiming.put("createAdvocator", insertTiming.get("createAdvocator") + MyTimer.durationSeconds());
                } else {
                    insertTiming.put("createAdvocator", MyTimer.durationSeconds());
                }
            }
            if (keepTiming) MyTimer.startTimer();
            advocatorsTree.insert(newAdvocator);
            if (keepTiming) MyTimer.stopTimer();
            if (keepTiming) {
                if (insertTiming.containsKey("insertTree")) {
                    insertTiming.put("insertTree", insertTiming.get("insertTree") + MyTimer.durationSeconds());
                } else {
                    insertTiming.put("insertTree", MyTimer.durationSeconds());
                }
            }
            this.advocatorClusters[zoom].add(c);

            // insert this cluster into lower level
            if (zoom >= 1) {
                Cluster parent = createCluster(c.getDimensionValue(0), c.getDimensionValue(1), c.id, c.numPoints);
                c.parentId = parent.id;
                c.parent = parent;
                /** in BiSuperCluster Class, use Cluster.children to store direct descendants pointers */
                parent.children.add(c);
                parent.advocatorCluster = c;
                insert(parent, zoom - 1);
            }
        }
        // if earlier advocators' groups could be merged into
        else {
            if (keepTiming) MyTimer.startTimer();
            // find the earliest advocator
            Advocator earliestAdvocator = null;
            for (Advocator advocator: advocators) {
                if (earliestAdvocator == null) {
                    earliestAdvocator = advocator;
                }
                else if (earliestAdvocator.id > advocator.id) {
                    earliestAdvocator = advocator;
                }
            }
            if (keepTiming) MyTimer.stopTimer();
            if (keepTiming) {
                if (insertTiming.containsKey("findEarliest")) {
                    insertTiming.put("findEarliest", insertTiming.get("findEarliest") + MyTimer.durationSeconds());
                } else {
                    insertTiming.put("findEarliest", MyTimer.durationSeconds());
                }
            }

            if (keepTiming) MyTimer.startTimer();
            // merge into earliest advocator's group
            Cluster cluster = earliestAdvocator.cluster;
            if (cluster.numPoints == 0) {
                cluster.numPoints = 1;
                cluster.id = (cluster.id << 5) + (maxZoom + 1);
            }
            double wx = cluster.getDimensionValue(0) * cluster.numPoints;
            double wy = cluster.getDimensionValue(1) * cluster.numPoints;
            wx += c.getDimensionValue(0) * (c.numPoints==0?1:c.numPoints);
            wy += c.getDimensionValue(1) * (c.numPoints==0?1:c.numPoints);
            cluster.numPoints = cluster.numPoints + (c.numPoints==0?1:c.numPoints);
            cluster.setDimensionValue(0, wx / cluster.numPoints);
            cluster.setDimensionValue(1, wy / cluster.numPoints);
            // merge c's children into cluster's children too
            if (zoom < maxZoom) {
                cluster.children.addAll(c.children);
                for (Cluster child: c.children) {
                    child.parentId = cluster.id;
                    child.parent = cluster;
                }
            }

            // merge c into cluster.parent recursively
            merge(cluster.parent, c);

            if (keepTiming) MyTimer.stopTimer();
            if (keepTiming) {
                if (insertTiming.containsKey("mergeCluster")) {
                    insertTiming.put("mergeCluster", insertTiming.get("mergeCluster") + MyTimer.durationSeconds());
                } else {
                    insertTiming.put("mergeCluster", MyTimer.durationSeconds());
                }
            }
        }
    }

    /**
     * Merge c2 into c1, and recursively merge c2 into c1.parent
     *
     * @param c1
     * @param c2
     */
    private void merge(Cluster c1, Cluster c2) {
        if (c1 == null) return;
        if (c1.numPoints == 0) {
            c1.numPoints = 1;
            c1.id = (c1.id << 5) + (maxZoom + 1);
        }
        double wx = c1.getDimensionValue(0) * c1.numPoints;
        double wy = c1.getDimensionValue(1) * c1.numPoints;
        wx += c2.getDimensionValue(0) * (c2.numPoints==0?1:c2.numPoints);
        wy += c2.getDimensionValue(1) * (c2.numPoints==0?1:c2.numPoints);
        c1.numPoints = c1.numPoints + (c2.numPoints==0?1:c2.numPoints);
        c1.setDimensionValue(0, wx / c1.numPoints);
        c1.setDimensionValue(1, wy / c1.numPoints);
        merge(c1.parent, c2);
    }

    /**
     * Get an array of Clusters for given visible region and zoom level
     * note: search on advocators tree, but return the clusters attached to the advocators
     *
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param zoom
     * @return
     */
    public Cluster[] getClusters(double x0, double y0, double x1, double y1, int zoom) {
        double minLng = ((x0 + 180) % 360 + 360) % 360 - 180;
        double minLat = Math.max(-90, Math.min(90, y0));
        double maxLng = x1 == 180 ? 180 : ((x1 + 180) % 360 + 360) % 360 - 180;
        double maxLat = Math.max(-90, Math.min(90, y1));

        if (x1 - x0 >= 360) {
            minLng = -180;
            maxLng = 180;
        } else if (minLng > maxLng) {
            Cluster[] easternHem = this.getClusters(minLng, minLat, 180, maxLat, zoom);
            Cluster[] westernHem = this.getClusters(-180, minLat, maxLng, maxLat, zoom);
            return concat(easternHem, westernHem);
        }

        KDTree<Advocator> advocatorsTree = this.advocatorsTrees[this._limitZoom(zoom)];
        Cluster leftBottom = createPointCluster(minLng, maxLat);
        Cluster rightTop = createPointCluster(maxLng, minLat);
        List<Advocator> advocators = advocatorsTree.range(leftBottom, rightTop);
        Cluster[] clusters = new Cluster[advocators.size()];
        int i = 0;
        for (Advocator advocator: advocators) {
            Cluster cluster = advocator.cluster.clone();
            cluster.setDimensionValue(0, xLng(cluster.getDimensionValue(0)));
            cluster.setDimensionValue(1, yLat(cluster.getDimensionValue(1)));
            clusters[i] = cluster;
            i ++;
        }
        //-DEBUG-//
//        System.out.println("[getClusters] advocatorsTree.size = " + advocatorsTree.size());
//        System.out.println("[getClusters] result.size = " + clusters.length);
//        System.out.println("[getClusters] this level advocatorClusters.size = " + this.advocatorClusters[this._limitZoom(zoom)].size());
//        System.out.println("[getClusters] result: ===============>");
//        for (Cluster result: clusters) {
//            System.out.println(result);
//        }
        //-DEBUG-//
        return clusters;
    }

    private void shift(Cluster c, Advocator from, int zoom) {
        if (isAdvocatorOfParent(c)) {
            // Find list of clusters that should merge into c.parent
            List<Cluster> toMerge = toMerge(c, zoom);
            //-DEBUG-//
//            if (!toMerge.isEmpty()) {
//                System.out.println("To merge (" + toMerge.size() + ") clusters:");
//                for (Cluster m : toMerge) {
//                    System.out.println("    " + m);
//                }
//            }
            //-DEBUG-//
            // Find list of clusters that should split from c.parent
            List<Cluster> toSplit = toSplit(c, zoom);
            //-DEBUG-//
//            if (!toSplit.isEmpty()) {
//                System.out.println("To split (" + toSplit.size() + ") clusters:");
//                for (Cluster s : toSplit) {
//                    System.out.println("     " + s);
//                }
//            }
            //-DEBUG-//

            if (keepTiming) MyTimer.startTimer();
            // shift the advocator "from"'s location to the centroid of cluster c
            //TODO - use a index structure that support update location to handle the position shift
            this.advocatorsTrees[zoom].delete(from);
            Advocator to = from.clone();
            to.setDimensionValue(0, c.getDimensionValue(0));
            to.setDimensionValue(1, c.getDimensionValue(1));
            c.advocator = to;
            this.advocatorsTrees[zoom].insert(to);
            if (keepTiming) MyTimer.stopTimer();
            if (keepTiming) {
                if (shiftTiming.containsKey("updateKDTree")) {
                    shiftTiming.put("updateKDTree", shiftTiming.get("updateKDTree") + MyTimer.durationSeconds());
                } else {
                    shiftTiming.put("updateKDTree", MyTimer.durationSeconds());
                }
            }

            for (Cluster m: toMerge) {
                if (keepTiming) MyTimer.startTimer();
                split(m.parent, m, zoom - 1);
                if (keepTiming) MyTimer.stopTimer();
                if (keepTiming) {
                    if (shiftTiming.containsKey("splitCluster")) {
                        shiftTiming.put("splitCluster", shiftTiming.get("splitCluster") + MyTimer.durationSeconds());
                    } else {
                        shiftTiming.put("splitCluster", MyTimer.durationSeconds());
                    }
                }

                c.parent.children.add(m);
                m.parent = c.parent;

                if (keepTiming) MyTimer.startTimer();
                merge(c.parent, m);
                if (keepTiming) MyTimer.stopTimer();
                if (keepTiming) {
                    if (shiftTiming.containsKey("mergeCluster")) {
                        shiftTiming.put("mergeCluster", shiftTiming.get("mergeCluster") + MyTimer.durationSeconds());
                    } else {
                        shiftTiming.put("mergeCluster", MyTimer.durationSeconds());
                    }
                }
            }

            for (Cluster s: toSplit) {
                if (keepTiming) MyTimer.startTimer();
                split(c.parent, s, zoom - 1);
                if (keepTiming) MyTimer.stopTimer();
                if (keepTiming) {
                    if (shiftTiming.containsKey("splitCluster")) {
                        shiftTiming.put("splitCluster", shiftTiming.get("splitCluster") + MyTimer.durationSeconds());
                    } else {
                        shiftTiming.put("splitCluster", MyTimer.durationSeconds());
                    }
                }

                Cluster parent = createCluster(s.getDimensionValue(0), s.getDimensionValue(1), s.id, s.numPoints);
                s.parentId = parent.id;
                s.parent = parent;
                parent.children.add(s);
                parent.advocatorCluster = s;

                if (keepTiming) MyTimer.startTimer();
                insert(parent, zoom - 1);
                if (keepTiming) MyTimer.stopTimer();
                if (keepTiming) {
                    if (shiftTiming.containsKey("reInsert")) {
                        shiftTiming.put("reInsert", shiftTiming.get("reInsert") + MyTimer.durationSeconds());
                    } else {
                        shiftTiming.put("reInsert", MyTimer.durationSeconds());
                    }
                }
            }
        }
        else {
            if (keepTiming) MyTimer.startTimer();
            split(c.parent, c, zoom - 1);
            if (keepTiming) MyTimer.stopTimer();
            if (keepTiming) {
                if (shiftTiming.containsKey("splitCluster")) {
                    shiftTiming.put("splitCluster", shiftTiming.get("splitCluster") + MyTimer.durationSeconds());
                } else {
                    shiftTiming.put("splitCluster", MyTimer.durationSeconds());
                }
            }

            Cluster parent = createCluster(c.getDimensionValue(0), c.getDimensionValue(1), c.id, c.numPoints);
            c.parentId = parent.id;
            c.parent = parent;
            parent.children.add(c);
            parent.advocatorCluster = c;

            if (keepTiming) MyTimer.startTimer();
            insert(parent, zoom - 1);
            if (keepTiming) MyTimer.stopTimer();
            if (keepTiming) {
                if (shiftTiming.containsKey("reInsert")) {
                    shiftTiming.put("reInsert", shiftTiming.get("reInsert") + MyTimer.durationSeconds());
                } else {
                    shiftTiming.put("reInsert", MyTimer.durationSeconds());
                }
            }
        }
    }

    /**
     * Check whether cluster c is an advocator of its parent
     *
     * @param c
     * @return
     */
    private boolean isAdvocatorOfParent(Cluster c) {
        if (c.parent != null && c == c.parent.advocatorCluster)
            return true;
        return false;
    }

    /**
     * Find list of clusters that should merge into c.parent
     *
     * @param c
     * @param zoom
     * @return
     */
    private List<Cluster> toMerge(Cluster c, int zoom) {
        if (keepTiming) MyTimer.startTimer();
        // shifting at level zoom affects clustering results of level (zoom-1)
        double r = getRadius(zoom - 1);
        if (keepTiming) MyTimer.stopTimer();
        if (keepTiming) {
            if (shiftTiming.containsKey("getRadius")) {
                shiftTiming.put("getRadius", shiftTiming.get("getRadius") + MyTimer.durationSeconds());
            } else {
                shiftTiming.put("getRadius", MyTimer.durationSeconds());
            }
        }

        if (keepTiming) MyTimer.startTimer();
        List<Cluster> clusters = this.clustersTree.within(c, r);
        if (keepTiming) MyTimer.stopTimer();
        if (keepTiming) {
            if (shiftTiming.containsKey("rangeSearch")) {
                shiftTiming.put("rangeSearch", shiftTiming.get("rangeSearch") + MyTimer.durationSeconds());
            } else {
                shiftTiming.put("rangeSearch", MyTimer.durationSeconds());
            }
        }

        clusters.removeAll(c.parent.children);
        // remove those already has a parent with smaller id of advocator than c.parent
        for (Iterator<Cluster> iter = clusters.iterator(); iter.hasNext(); ) {
            Cluster curr = iter.next();
            if (curr.parent != null) {
                Cluster currParent = curr.parent;
                if (currParent.advocator != null) {
                    if (currParent.advocator.id <= c.parent.advocator.id) {
                        iter.remove();
                    }
                }
            }
        }
        return clusters;
    }

    /**
     * Find list of clusters that should split from c.parent
     *
     * @param c
     * @param zoom
     * @return
     */
    private List<Cluster> toSplit(Cluster c, int zoom) {
        if (keepTiming) MyTimer.startTimer();
        // shifting at level zoom affects clustering results of level (zoom-1)
        double r = getRadius(zoom - 1);
        if (keepTiming) MyTimer.stopTimer();
        if (keepTiming) {
            if (shiftTiming.containsKey("getRadius")) {
                shiftTiming.put("getRadius", shiftTiming.get("getRadius") + MyTimer.durationSeconds());
            } else {
                shiftTiming.put("getRadius", MyTimer.durationSeconds());
            }
        }

        if (keepTiming) MyTimer.startTimer();
        List<Cluster> clusters = this.clustersTree.within(c, r);
        if (keepTiming) MyTimer.stopTimer();
        if (keepTiming) {
            if (shiftTiming.containsKey("rangeSearch")) {
                shiftTiming.put("rangeSearch", shiftTiming.get("rangeSearch") + MyTimer.durationSeconds());
            } else {
                shiftTiming.put("rangeSearch", MyTimer.durationSeconds());
            }
        }

        //-DEBUG-//
//        System.out.println("The following " + clusters.size() + " clusters are covered by " + c + " within radius = " + r);
//        for (Cluster _c: clusters) {
//            System.out.println("    " + _c);
//        }
        //-DEBUG-//
        List<Cluster> children = new ArrayList<>(c.parent.children);
        //-DEBUG-//
//        System.out.println("The following " + children.size() + " clusters are children of " + c);
//        for (Cluster _c: children) {
//            System.out.println("    " + _c);
//        }
        //-DEBUG-//
        children.removeAll(clusters);
        return children;
    }

    /**
     * Split c2 from c1, zoom is the level of c1,
     * and recursively split c2 from c1.parent
     *
     * @param c1
     * @param c2
     * @param zoom
     */
    private void split(Cluster c1, Cluster c2, int zoom) {
        if (c1 == null) return;
        // if c2 is c1's only child, delete c1 directly
        if (c1.children.size() == 1 && c1.children.contains(c2)) {
            this.advocatorsTrees[zoom].delete(c1.advocator);
            this.advocatorClusters[zoom].remove(c1);
            split(c1.parent, c1, zoom - 1);
        }
        // otherwise, subtract c2's weight from c1 and recurse
        else {
            double wx = c1.getDimensionValue(0) * c1.numPoints;
            double wy = c1.getDimensionValue(1) * c1.numPoints;
            wx -= c2.getDimensionValue(0) * (c2.numPoints == 0 ? 1 : c2.numPoints);
            wy -= c2.getDimensionValue(1) * (c2.numPoints == 0 ? 1 : c2.numPoints);
            c1.numPoints = c1.numPoints - (c2.numPoints == 0 ? 1 : c2.numPoints);
            c1.setDimensionValue(0, wx / c1.numPoints);
            c1.setDimensionValue(1, wy / c1.numPoints);
            c1.children.remove(c2);
            split(c1.parent, c2, zoom - 1);
        }
    }

    public void printTiming() {
        System.out.println("Timing distribution:");
        System.out.println("    [insert] " + insertTiming.get("total") + " seconds");
        System.out.println("        [get radius] " + insertTiming.get("getRadius") + " seconds");
        System.out.println("        [range search] " + insertTiming.get("rangeSearch") + " seconds");
        System.out.println("        [create advocator] " + insertTiming.get("createAdvocator") + " seconds");
        System.out.println("        [insert tree] " + insertTiming.get("insertTree") + " seconds");
        System.out.println("        [find earliest] " + insertTiming.get("findEarliest") + " seconds");
        System.out.println("        [merge cluster] " + insertTiming.get("mergeCluster") + " seconds");
        System.out.println("    [shift] " + shiftTiming.get("total") + " seconds");
        System.out.println("        [get radius] " + shiftTiming.get("getRadius") + " seconds");
        System.out.println("        [build KDTree] " + shiftTiming.get("buildKDTree") + " seconds");
        System.out.println("        [shift] " + shiftTiming.get("shift") + " seconds");
        System.out.println("            [range search] " + shiftTiming.get("rangeSearch") + " seconds");
        System.out.println("            [update KDTree] " + shiftTiming.get("updateKDTree") + " seconds");
        System.out.println("            [split cluster] " + shiftTiming.get("splitCluster") + " seconds");
        System.out.println("            [merge cluster] " + shiftTiming.get("mergeCluster") + " seconds");
        System.out.println("            [re-insert] " + shiftTiming.get("reInsert") + " seconds");
    }
}
