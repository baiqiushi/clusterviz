package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Request {
    /**
     * - query
     *   - Query existing clusters for given keyword
     *
     * - cmd
     *   - Execute commands
     *
     * - transfer
     *   - Transfer raw data of given keyword in GeoJson format
     *
     * - analysis
     *   - Query statistics of clusters for given keyword
     */
    public String type; // "query"/"cmd"/"transfer"/"analysis"
    public String id;
    public String keyword;
    public Query query;
    public Command[] cmds;
    public Analysis analysis;
}
