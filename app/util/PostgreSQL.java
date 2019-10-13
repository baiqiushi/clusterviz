package util;

import model.PointTuple;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class PostgreSQL {

    public Connection conn = null;

    public String url = "jdbc:postgresql://localhost/twitter";
    public String username = "postgres";
    public String password = "postgres";

    public boolean connectDB() {
        try {
            conn = DriverManager.getConnection(url, username, password);
            System.out.println("Connected to the PostgreSQL server successfully.");
            return true;
        } catch (SQLException e) {
            System.err.println("Connecting to the PostgreSQL server failed. Exceptions:");
            System.err.println(e.getMessage());
            return false;
        }
    }

    public void disconnectDB() {
        try {
            conn.close();
            System.out.println("Disconnected from the PostgreSQL server successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public PointTuple[] queryPointTuplesForKeyword(String keyword) {

        if (this.conn == null) {
            if(!this.connectDB()) {
                return null;
            }
        }

        System.out.println("Querying PostgreSQL with keyword: [" + keyword + "] ... ...");
        List<PointTuple> result = new ArrayList<PointTuple>();
        String sql = "SELECT create_at, x, y, id FROM tweets WHERE to_tsvector('english', text)@@to_tsquery('english', ?)";
        long start = System.nanoTime();
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, keyword);
            System.out.println("SQL: " + statement);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Timestamp create_at = rs.getTimestamp(1);
                Double x = rs.getDouble(2);
                Double y = rs.getDouble(3);
                long tid = rs.getLong(4);
                PointTuple pt = new PointTuple(2);
                pt.timestamp = create_at;
                pt.setDimensionValue(0, x);
                pt.setDimensionValue(1, y);
                pt.tid = tid;
                result.add(pt);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        long end = System.nanoTime();
        System.out.println("Querying PostgreSQL with keyword: [" + keyword + "] is done! ");
        System.out.println("Takes time: " + TimeUnit.SECONDS.convert(end - start, TimeUnit.NANOSECONDS) + " seconds");
        System.out.println("Result size: " + result.size());
        return result.toArray(new PointTuple[result.size()]);
    }

    public PointTuple[] queryPointTuplesForKeywordAndTime(String keyword, Date sd, Date ed) {

        if (this.conn == null) {
            if(!this.connectDB()) {
                return null;
            }
        }

        System.out.println("Querying PostgreSQL with keyword: [" + keyword + "] and time [" + sd + ", " + ed + "]... ...");
        List<PointTuple> result = new ArrayList<PointTuple>();
        String sql = "SELECT create_at, x, y, id FROM tweets WHERE to_tsvector('english', text)@@to_tsquery('english', ?) and create_at between ? and ?";
        long start = System.nanoTime();
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, keyword);
            statement.setTimestamp(2, new Timestamp(sd.getTime()));
            statement.setTimestamp(3, new Timestamp(ed.getTime()));
            System.out.println("SQL: " + statement);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Timestamp create_at = rs.getTimestamp(1);
                Double x = rs.getDouble(2);
                Double y = rs.getDouble(3);
                long tid = rs.getLong(4);
                PointTuple pt = new PointTuple(2);
                pt.timestamp = create_at;
                pt.setDimensionValue(0, x);
                pt.setDimensionValue(1, y);
                pt.tid = tid;
                result.add(pt);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        long end = System.nanoTime();
        System.out.println("Querying PostgreSQL with keyword: [" + keyword + "] and time [" + sd + ", " + ed +  "] is done! ");
        System.out.println("Takes time: " + TimeUnit.SECONDS.convert(end - start, TimeUnit.NANOSECONDS) + " seconds");
        System.out.println("Result size: " + result.size());
        return result.toArray(new PointTuple[result.size()]);
    }

    public PointTuple[] queryPointTuplesForTime(Date sd, Date ed) {

        if (this.conn == null) {
            if(!this.connectDB()) {
                return null;
            }
        }

        System.out.println("Querying PostgreSQL with time [" + sd + ", " + ed + "]... ...");
        List<PointTuple> result = new ArrayList<PointTuple>();
        String sql = "SELECT create_at, x, y, id FROM tweets WHERE create_at between ? and ?";
        long start = System.nanoTime();
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setTimestamp(1, new Timestamp(sd.getTime()));
            statement.setTimestamp(2, new Timestamp(ed.getTime()));
            System.out.println("SQL: " + statement);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Timestamp create_at = rs.getTimestamp(1);
                Double x = rs.getDouble(2);
                Double y = rs.getDouble(3);
                long tid = rs.getLong(4);
                PointTuple pt = new PointTuple(2);
                pt.timestamp = create_at;
                pt.setDimensionValue(0, x);
                pt.setDimensionValue(1, y);
                pt.tid = tid;
                result.add(pt);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        long end = System.nanoTime();
        System.out.println("Querying PostgreSQL with time [" + sd + ", " + ed +  "] is done! ");
        System.out.println("Database time: " + TimeUnit.SECONDS.convert(end - start, TimeUnit.NANOSECONDS) + " seconds");
        System.out.println("Result size: " + result.size());
        return result.toArray(new PointTuple[result.size()]);
    }

    public PointTuple[] queryPointTuplesForLimit(int limit) {
        if (this.conn == null) {
            if(!this.connectDB()) {
                return null;
            }
        }

        System.out.println("Querying PostgreSQL with limit: [" + limit + "] ... ...");
        List<PointTuple> result = new ArrayList<PointTuple>();
        String sql = "SELECT create_at, x, y, id FROM tweets limit ?";
        long start = System.nanoTime();
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setInt(1, limit);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Timestamp create_at = rs.getTimestamp(1);
                Double x = rs.getDouble(2);
                Double y = rs.getDouble(3);
                long tid = rs.getLong(4);
                PointTuple pt = new PointTuple(2);
                pt.timestamp = create_at;
                pt.setDimensionValue(0, x);
                pt.setDimensionValue(1, y);
                pt.tid = tid;
                result.add(pt);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        long end = System.nanoTime();
        System.out.println("Querying PostgreSQL with limit: [" + limit + "] is done! ");
        System.out.println("Takes time: " + TimeUnit.SECONDS.convert(end - start, TimeUnit.NANOSECONDS) + " seconds");
        System.out.println("Result size: " + result.size());
        return result.toArray(new PointTuple[result.size()]);
    }
}
