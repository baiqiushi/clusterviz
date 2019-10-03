package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Query {
    public String cluster; // key of the cluster
    public int zoom;
    public double[] bbox; //x0, y0, x1, y1
}
