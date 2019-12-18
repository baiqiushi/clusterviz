package util;

import model.PointTuple;
import model.PointTupleListFactory;

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

    public List<PointTuple> queryPointTuplesForKeyword(String keyword) {

        if (this.conn == null) {
            if(!this.connectDB()) {
                return null;
            }
        }

        System.out.println("Querying PostgreSQL with keyword: [" + keyword + "] ... ...");
        //List<PointTuple> result = new ArrayList<PointTuple>();
        // reuse array list of PointTuple
        List<PointTuple> result = PointTupleListFactory.newPointTupleList();
        int i = 0;
        String sql = "SELECT x, y, id FROM tweets WHERE to_tsvector('english', text)@@to_tsquery('english', ?)";
        long start = System.nanoTime();
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, keyword);
            System.out.println("SQL: " + statement);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Double x = rs.getDouble(1);
                Double y = rs.getDouble(2);
                long tid = rs.getLong(3);
                // still enough objects in list can be reused
                if (i < result.size()) {
                    result.get(i).setX(x);
                    result.get(i).setY(y);
                    result.get(i).tid = tid;
                }
                // no more objects can be reused
                else {
                    PointTuple pt = new PointTuple();
                    pt.setX(x);
                    pt.setY(y);
                    pt.tid = tid;
                    result.add(pt);
                }
                i ++;
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        // remove additional objects in the tail of the list
        while (i < result.size()) {
            result.remove(result.size() - 1);
        }
        long end = System.nanoTime();
        System.out.println("Querying PostgreSQL with keyword: [" + keyword + "] is done! ");
        System.out.println("Takes time: " + TimeUnit.SECONDS.convert(end - start, TimeUnit.NANOSECONDS) + " seconds");
        System.out.println("Result size: " + result.size());
        return result;
    }

    public List<PointTuple> queryPointTuplesForKeywordAndTime(String keyword, Date sd, Date ed) {

        if (this.conn == null) {
            if(!this.connectDB()) {
                return null;
            }
        }

        System.out.println("Querying PostgreSQL with keyword: [" + keyword + "] and time [" + sd + ", " + ed + "]... ...");
        //List<PointTuple> result = new ArrayList<PointTuple>();
        // reuse array list of PointTuple
        List<PointTuple> result = PointTupleListFactory.newPointTupleList();
        int i = 0;
        //-DEBUG-//
        int size = result.size();
        if (result.size() > 0) {
            System.out.println("[Debug] [PostgreSQL] is reusing List<PointTuple> with size = " + result.size());
        }
        //-DEBUG-//
        String sql = "SELECT x, y, id FROM tweets WHERE to_tsvector('english', text)@@to_tsquery('english', ?) and create_at between ? and ?";
        long start = System.nanoTime();
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, keyword);
            statement.setTimestamp(2, new Timestamp(sd.getTime()));
            statement.setTimestamp(3, new Timestamp(ed.getTime()));
            System.out.println("SQL: " + statement);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Double x = rs.getDouble(1);
                Double y = rs.getDouble(2);
                long tid = rs.getLong(3);
                // still enough objects in list can be reused
                if (i < result.size()) {
                    result.get(i).setX(x);
                    result.get(i).setY(y);
                    result.get(i).tid = tid;
                }
                // no more objects can be reused
                else {
                    PointTuple pt = new PointTuple();
                    pt.setX(x);
                    pt.setY(y);
                    pt.tid = tid;
                    result.add(pt);
                }
                i ++;
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        //-DEBUG-//
        if (result.size() > size) {
            System.out.println("[Debug] [PostgreSQL] " + (result.size() - size)  + " more PointTuple objects were created to complement the reused list.");
        }
        //-DEBUG-//
        // remove additional objects in the tail of the list
        while (i < result.size()) {
            result.remove(result.size() - 1);
        }
        long end = System.nanoTime();
        System.out.println("Querying PostgreSQL with keyword: [" + keyword + "] and time [" + sd + ", " + ed +  "] is done! ");
        System.out.println("Takes time: " + TimeUnit.SECONDS.convert(end - start, TimeUnit.NANOSECONDS) + " seconds");
        System.out.println("Result size: " + result.size());
        return result;
    }

    public List<PointTuple> queryPointTuplesForTime(Date sd, Date ed) {

        if (this.conn == null) {
            if(!this.connectDB()) {
                return null;
            }
        }

        System.out.println("Querying PostgreSQL with time [" + sd + ", " + ed + "]... ...");
        //List<PointTuple> result = new ArrayList<PointTuple>();
        List<PointTuple> result = PointTupleListFactory.newPointTupleList();
        int i = 0;
        //-DEBUG-//
        int size = result.size();
        if (result.size() > 0) {
            System.out.println("[Debug] [PostgreSQL] is reusing List<PointTuple> with size = " + result.size());
        }
        //-DEBUG-//
        String sql = "SELECT x, y, id FROM tweets WHERE create_at between ? and ?";
        long start = System.nanoTime();
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setTimestamp(1, new Timestamp(sd.getTime()));
            statement.setTimestamp(2, new Timestamp(ed.getTime()));
            System.out.println("SQL: " + statement);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Double x = rs.getDouble(1);
                Double y = rs.getDouble(2);
                long tid = rs.getLong(3);
                // still enough objects in list can be reused
                if (i < result.size()) {
                    result.get(i).setX(x);
                    result.get(i).setY(y);
                    result.get(i).tid = tid;
                }
                // no more objects can be reused
                else {
                    PointTuple pt = new PointTuple();
                    pt.setX(x);
                    pt.setY(y);
                    pt.tid = tid;
                    result.add(pt);
                }
                i ++;
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        //-DEBUG-//
        if (result.size() > size) {
            System.out.println("[Debug] [PostgreSQL] " + (result.size() - size)  + " more PointTuple objects were created to complement the reused list.");
        }
        //-DEBUG-//
        // remove additional objects in the tail of the list
        while (i < result.size()) {
            result.remove(result.size() - 1);
        }
        long end = System.nanoTime();
        System.out.println("Querying PostgreSQL with time [" + sd + ", " + ed +  "] is done! ");
        System.out.println("Database time: " + TimeUnit.SECONDS.convert(end - start, TimeUnit.NANOSECONDS) + " seconds");
        System.out.println("Result size: " + result.size());
        return result;
    }

    public PointTuple[] queryPointTuplesForLimit(int limit) {
        if (this.conn == null) {
            if(!this.connectDB()) {
                return null;
            }
        }

        System.out.println("Querying PostgreSQL with limit: [" + limit + "] ... ...");
        List<PointTuple> result = new ArrayList<PointTuple>();
        String sql = "SELECT x, y, id FROM tweets limit ?";
        long start = System.nanoTime();
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setInt(1, limit);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Double x = rs.getDouble(1);
                Double y = rs.getDouble(2);
                long tid = rs.getLong(3);
                PointTuple pt = new PointTuple();
                pt.setX(x);
                pt.setY(y);
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
