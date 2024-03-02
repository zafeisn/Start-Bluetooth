package online.done.sea.util;
/**
 * JDBC工具类
 * 1、实现MySQL数据库的连接
 * 2、添加数据到start_ble表中
 */

import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JDBCUtil {

    // 准备驱动
    Connection conn = null;
    // 预编译
    PreparedStatement ps = null;
    // 结果集
    ResultSet rs = null;
    // 数据库连接url
    //private String url = "jdbc:mysql://119.29.179.102:3306/lab_database?characterEncoding=UTF-8&useSSL=false";
    private String url = "jdbc:mysql://175.178.104.65:3306/lab_database?characterEncoding=UTF-8&useSSL=false";


    // 连接数据库
    public Connection getConn(){
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(url, "LinJie", "LinJie5!");
            conn.setAutoCommit(false);
            Log.i("连接","数据库连接成功");
        } catch (ClassNotFoundException e) {
            Log.i("驱动","驱动问题");
            e.printStackTrace();
        } catch (SQLException e) {
            Log.i("连接","数据库连接失败");
            e.printStackTrace();
        }
        return conn;
    }

    /**
     * 插入数据到t_rssi表（离线采集）
     * @param conn
     * @return
     */
    public PreparedStatement getRssiPS(Connection conn){
        if (conn != null){

            String sql = "insert into t_rssi(X,Y,Z,N1,N2,N3,N4,N5,N6,N7,N8,W1,W2,W3,W4,W5,W6,W7,W8,W9,W0," +
                    "F1,F2,Electricity,Start_Time,Phone_MAC,Phone_Brand,Phone_Android,Scan_Duration,Scan_Interval," +
                    "Tx_Power) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            try {
                ps = conn.prepareStatement(sql);
                Log.i("执行","SQL语句执行成功");
            } catch (SQLException e) {
                Log.i("SQL","SQL语句有问题");
                e.printStackTrace();
            }
        } else {
            Log.i("conn", "数据库连接失败");
        }
        return ps;
    }

    /**
     * 查询t_user表（登录）
     * @param conn
     * @return
     */
    public PreparedStatement selectUser(Connection conn){
        if (conn != null) {
            String sql = "select * from t_user where username=? and password=?";
            try {
                ps = conn.prepareStatement(sql);
                Log.i("执行", "SQL语句执行成功");
            } catch (SQLException e) {
                Log.i("SQL", "SQL语句有问题");
                e.printStackTrace();
            }
        } else {
            Log.i("conn", "数据库连接失败");
        }
        return ps;
    }

    /**
     * 插入到t_online表（在线定位）
     * @param conn
     * @return
     */
    public PreparedStatement getOnlinePS(Connection conn){
        if (conn != null){
            String sql = "insert into t_online(N1,N2,N3,N4,N5,N6,N7,N8,W1,W2,W3,W4,W5,W6,W7,W8,W9,W0,F1,F2," +
                    "Electricity,Start_Time,Phone_MAC,Phone_Brand,Phone_Android,Scan_Duration,Scan_Interval," +
                    "Tx_Power) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            try {
                ps = conn.prepareStatement(sql);
                Log.i("执行","SQL语句执行成功");
            } catch (SQLException e) {
                Log.i("SQL","SQL语句有问题");
                e.printStackTrace();
            }
        } else {
            Log.i("conn", "数据库连接失败");
        }
        return ps;
    }

    /**
     * 将定位结果和处理数据写入数据库中
     * @param conn
     * @return
     */
    public PreparedStatement getLocationPS(Connection conn){
        if (conn != null){
            String sql = "insert into t_location(N1,N2,N3,N4,N5,N6,N7,N8,W1,W2,W3,W4,W5,W6,W7,W8,W9,W0,F1,F2," +
                    "Electricity,Start_Time,Phone_MAC,Phone_Brand,Phone_Android,Scan_Duration,Scan_Interval," +
                    "Tx_Power,X,Y) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            try {
                ps = conn.prepareStatement(sql);
                Log.i("执行","SQL语句执行成功");
            } catch (SQLException e) {
                Log.i("SQL","SQL语句有问题");
                e.printStackTrace();
            }
        } else {
            Log.i("conn", "数据库连接失败");
        }
        return ps;
    }

    /**
     * 释放资源
     */
    public void close(){
        if (rs != null){
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (ps != null){
            try {
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (conn != null){
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        System.out.println("关闭！");
    }
}
