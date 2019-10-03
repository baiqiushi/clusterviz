package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Analysis {
    public String cluster; // key of the cluster
    public int zoom;
    public int p1; // point1 id
    public int p2; // point2 id
}
