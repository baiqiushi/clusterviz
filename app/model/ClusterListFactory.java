package model;

import java.util.ArrayList;
import java.util.List;

public class ClusterListFactory {
    static List<Cluster> inventory = null;

    static public List<Cluster> newClusterList() {
        if (inventory == null) {
            return new ArrayList<>();
        }
        else {
            List<Cluster> result = inventory;
            inventory = null;
            return result;
        }
    }

    static public void recycle(List<Cluster> cs) {
        if (inventory == null) {
            inventory = cs;
        }
        else {
            inventory.addAll(cs);
        }
    }
}
