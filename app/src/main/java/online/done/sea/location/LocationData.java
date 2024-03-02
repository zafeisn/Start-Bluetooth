package online.done.sea.location;

import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Message;
import android.util.Log;
import androidx.fragment.app.FragmentActivity;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import online.done.sea.util.BluetoothUtil;
import online.done.sea.util.JDBCUtil;
import online.done.sea.util.MyHandler;
import online.done.sea.util.MyUtil;

public class LocationData {

    /**
     * 1、蓝牙信息集合以及在线阶段采集到的RSSI数组
     * 2、实例化蓝牙工具类，以便后面调用
     * 3、蓝牙管理器
     * 4、handler处理
     */
    public Map<String,String> blueMap;
    private int [][] onlineRSSI;
    private int [] indexRSSI;
    private int [][] offElectricityArray = null;
    BluetoothUtil bluetoothUtil = new BluetoothUtil();
    BluetoothManager bluetoothManager;
    MyUtil myUtil = new MyUtil();

    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    JDBCUtil jdbcUtil = new JDBCUtil();
    float temp[];  // 归一化数据
    static String[] electricity = null;

    /**
     * 定位阶段扫描蓝牙信号
     * @param activity
     */
    public void startScan(FragmentActivity activity, MyHandler myHandler){
        // 设置handler
        myUtil.setHandler(myHandler);
        // 进行扫描初始化
        bluetoothUtil.initBLE();
        // 得到基本信息集合
        blueMap = BluetoothUtil.moduleMap;
        // 获取蓝牙管理器
        bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        // 开始扫描
        bluetoothUtil.startScanning(bluetoothManager,activity,myUtil);
//        Log.i("startScan","我比较快一点！");
    }

    /**
     * 将得到的原始数据进行简单数据处理后
     * 再将其转换成模型输入维度进行定位
     * @param rssi
     * @return
     */
    public float[][][] inputInit(int[][] rssi, int featureNum){
        float[][][] input = new float[1][1][featureNum];
        int n = rssi.length;
        temp = new float[featureNum];

        // 求平均
        for (int i=0; i<featureNum; i++){
            float argNumber = 0;
            int div = 0;
            for (int j=0; j<rssi[i].length; j++){
                if(rssi[i][j] == -110) {
                    continue;
                }
                div++;
                argNumber += rssi[i][j];
            }
//            temp[i] = argNumber / (if div== 0 : 1);
            temp[i] = div == 0 ? -110 : (argNumber / div);
        }
//        System.out.println("temp长度为：" + temp.length);
        // 数据归一化
        for (int i = 0; i < temp.length; i++) {
            if (temp[i] <= 0){
                temp[i] = (temp[i] + 110) / 110;
            } else {
                temp[i] = -(temp[i] - 110) / 110;
            }
        }
//        System.out.println(Arrays.toString(temp));
        // 构造输入维度
        for (int i = 0; i < temp.length; i++) {
            input[0][0][i] = temp[i];
        }
        return input;
    }

    /**
     * 在线定位阶段采集数据进行定位，写入数据库数据表t_online中存储
     */
    public void insertOnline(){
        indexRSSI = BluetoothUtil.rssiIndex;
        onlineRSSI = BluetoothUtil.rssiArray;
        offElectricityArray = BluetoothUtil.electricityArray;
        electricity = myUtil.getElectricity(indexRSSI, offElectricityArray);
        new Thread(new Runnable() {
            @Override
            public void run() {
                conn = jdbcUtil.getConn();
                ps = jdbcUtil.getOnlinePS(conn);
                int max = myUtil.getMax(indexRSSI);
                int min = myUtil.getMin(indexRSSI);
                System.out.println("max：" + max + " min：" + min + " sum/2=" + (max+min)/2);
                for (int i=0; i<max; i++) {
                    try {
                        for (int j = 0; j<indexRSSI.length; j++) {
                            ps.setString(j+1, String.valueOf(onlineRSSI[j][i]));
                        }
                        // 2023/2/17已测试成功
                        ps.setString(21, electricity[i]);
                        ps.setString(22, blueMap.get("startTime"));
                        ps.setString(23, blueMap.get("localMAC"));
                        ps.setString(24, blueMap.get("product"));
                        ps.setString(25, blueMap.get("version"));
                        ps.setString(26, blueMap.get("scanDuration"));
                        ps.setString(27, blueMap.get("periodicAdvertisingInterval"));
                        ps.setString(28, blueMap.get("txPower"));
                        ps.addBatch();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    ps.executeBatch();
                    conn.commit();
                    ps.clearBatch();
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    jdbcUtil.close();
                }

            }
        }).start();
    }

    /**
     * 将定位结果写入t_location数据表中
     * @param location
     */
    public void insertLocation(float location[]){
        System.out.println(Arrays.toString(location));
        blueMap = BluetoothUtil.moduleMap;
        new Thread(new Runnable() {
            @Override
            public void run() {

                conn = jdbcUtil.getConn();
                ps = jdbcUtil.getLocationPS(conn);
                try {
                    for (int j = 0; j < temp.length; j++) {
                        ps.setString(j+1, String.valueOf((int)(temp[j]*110-110)));
                    }
                    for (int i = temp.length; i < 20; i++) {
                        ps.setString(i+1, String.valueOf(-110));
                    }
                    ps.setString(21, electricity[0]);
                    ps.setString(22, blueMap.get("startTime"));
                    ps.setString(23, blueMap.get("localMAC"));
                    ps.setString(24, blueMap.get("product"));
                    ps.setString(25, blueMap.get("version"));
                    ps.setString(26, blueMap.get("scanDuration"));
                    ps.setString(27, blueMap.get("periodicAdvertisingInterval"));
                    ps.setString(28, blueMap.get("txPower"));
                    ps.setString(29, String.valueOf(4.2*location[0]/5));
                    ps.setString(30, String.valueOf(7*location[1]/10));
                    ps.addBatch();
                    ps.executeBatch();
                    conn.commit();
                    ps.clearBatch();
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    jdbcUtil.close();
                }

            }
        }).start();
    }

}
