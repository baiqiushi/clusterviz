package model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PointTuple extends Point {
    public static SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd hh:mm:ss");

    public int id;
    public Date timestamp;

    public PointTuple(int k) {
        super(k);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{timestamp: " + ft.format(timestamp) + ", coordinates: ");
        sb.append(super.toString());
        sb.append("}");
        return sb.toString();
    }
}
