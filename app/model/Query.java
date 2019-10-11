package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Query {
    public String cluster; // key of the cluster
    public String order; // if the given cluster does not exist, order will be used to build the cluster, the same as order definition in Command
    public int zoom;
    public double[] bbox; //x0, y0, x1, y1
    public String algorithm; // "SuperCluster"(default) / "SuperClusterInBatch" / "iSuperCluster"
}
