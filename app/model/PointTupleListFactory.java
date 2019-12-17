package model;

import java.util.ArrayList;
import java.util.List;

public class PointTupleListFactory {
    static List<PointTuple> inventory = null;

    static public List<PointTuple> newPointTupleList() {
        if (inventory == null) {
            return new ArrayList<>();
        }
        else {
            List<PointTuple> result = inventory;
            inventory = null;
            return result;
        }
    }

    static public void recycle(List<PointTuple> pts) {
        //-DEBUG-//
        //System.out.println("[Debug] [PointTupleListFactory] is recycling a list of PointTuples with size = " + pts.size());
        //-DEBUG-//
        if (inventory == null) {
            inventory = pts;
        }
        else {
            inventory.addAll(pts);
        }
    }
}
