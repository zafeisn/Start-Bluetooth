package online.done.sea.bluetooth;
/**
 * 【离线采集】
 * 1、用户填写坐标，同时填写扫描时长（默认为10秒）
 * 2、填写指定的蓝牙设备名称进行扫描（默认为RDL52810）
 * 3、扫描开始时，在提示框中给出扫描信息（坐标位置、扫描时长、设备名称）
 * 4、扫描结束后，在提示框中继续添加扫描信息
 * 5、若添加扫描条件，优先显示扫描条件
 */

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import online.done.sea.R;
import online.done.sea.util.BluetoothUtil;
import online.done.sea.util.JDBCUtil;
import online.done.sea.util.MyHandler;
import online.done.sea.util.MyUtil;

public class BluetoothActivity extends AppCompatActivity {

    /**
     * 1、蓝牙信息集合以及离线阶段采集到的RSSI数组
     * 2、实例化蓝牙工具类，以便后面调用
     * 3、蓝牙管理器
     * 4、handler处理
     */
    private Map<String,String> blueMap;
    private int [][] offRssiArray = null;
    private int [][] offElectricityArray = null;
    String[] electricity = null;
    private int [] indexRSSI;
    BluetoothUtil bluetoothUtil = new BluetoothUtil();
    BluetoothManager bluetoothManager;
    MyUtil myUtil = new MyUtil();

    /**
     * 申明控件
     */
    EditText ed_x,ed_y,ed_z,ed_name,ed_times,ed_duration;
    Button bt_open,bt_scan,bt_close,bt_add;
    TextView tv_content;

    /**
     * 数据库工具类
     */
    private Connection conn = null;
    private PreparedStatement ps = null;
    boolean content = false;
    JDBCUtil jdbcUtil = new JDBCUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_bluetooth);

        myUtil.setHandler(mHandler);

        init();
        setBleListener();

    }

    /**
     * 1、初始化控件、组件，并加载音频文件
     */
    private void init(){
        bluetoothUtil.initBLE();
        blueMap = BluetoothUtil.moduleMap;
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        ed_x = findViewById(R.id.ed_x);
        ed_y = findViewById(R.id.ed_y);
        ed_z = findViewById(R.id.ed_z);
        ed_name = findViewById(R.id.ed_name);
        ed_times = findViewById(R.id.ed_times);
        ed_duration = findViewById(R.id.ed_duration);
        bt_open = findViewById(R.id.bt_open);
        bt_scan = findViewById(R.id.bt_scan);
        bt_close = findViewById(R.id.bt_close);
        bt_add = findViewById(R.id.bt_add);
        tv_content = findViewById(R.id.tv_content);
    }

    /**
     * 2、创建监听事件
     */
    private void setBleListener(){
        OnClick onClick = new OnClick();
        bt_open.setOnClickListener(onClick);
        bt_scan.setOnClickListener(onClick);
        bt_close.setOnClickListener(onClick);
        bt_add.setOnClickListener(onClick);
    }


    /**
     * 添加点击事件
     * 1、开启蓝牙
     * 2、关闭蓝牙
     * 3、开始扫描
     */
    class OnClick implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.bt_open:
                    bluetoothUtil.isEnableBLE(BluetoothActivity.this);
                    break;
                case R.id.bt_close:
                    bluetoothUtil.disEnableBLE(BluetoothActivity.this);
                    break;
                case R.id.bt_scan:
                    bluetoothUtil.startScanning(bluetoothManager, isXYNull(), BluetoothActivity.this, myUtil);
                    break;
                case R.id.bt_add:
                    setScanReq();
                    break;
            }
        }
    }

    /**
     * 3、（离线阶段）判断XYZ输入是否为空
     * 若输入为空，则不能进行扫描，给出提示，返回false
     */
    public boolean isXYNull(){
        boolean temp = false;
        blueMap.put("X",ed_x.getText().toString());
        blueMap.put("Y",ed_y.getText().toString());
        blueMap.put("Z",ed_z.getText().toString());
        if ("".equals(blueMap.get("X"))){
            Toast.makeText(this,"X坐标还未输入-请填写完整，否则不能开始扫描哦~",Toast.LENGTH_SHORT).show();
        } else if ("".equals(blueMap.get("Y"))){
            Toast.makeText(this,"Y坐标还未输入-请填写完整，否则不能开始扫描哦~",Toast.LENGTH_SHORT).show();
        } else if ("".equals(blueMap.get("Z"))){
            Toast.makeText(this,"Z坐标还未输入-请填写完整，否则不能开始扫描哦~",Toast.LENGTH_SHORT).show();
        } else {
            temp = true;
        }
        return temp;
    }

    /**
     * 4、添加扫描条件
     * 时长、扫描次数以及设备名称
     */
    public void setScanReq(){
        String deviceName = ed_name.getText().toString();
        String duration = ed_duration.getText().toString();
        if (!("".equals(deviceName))){
            blueMap.put("deviceName",deviceName);
        }
        if (!("".equals(duration))){
            blueMap.put("duration",duration);
        }
        tv_content.setText("duration：" + blueMap.get("duration") + "s" + "\n"
                + "deviceName：" + blueMap.get("deviceName"));
        content = true;
    }

    /**
     * 5、添加到数据库中
     */
    public void insert(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                conn = jdbcUtil.getConn();
                ps = jdbcUtil.getRssiPS(conn);
                int max = myUtil.getMax(indexRSSI);
                int min = myUtil.getMin(indexRSSI);
                int count = (max + min) / 2;
                if (min == 0){
                    Message message = new Message();
                    message.what = 5;
                    mHandler.sendMessage(message);
                }
                // 添加写入数据库个数
                for (int i=0; i<max; i++) {
                    try {
                        ps.setString(1, blueMap.get("X"));
                        ps.setString(2, blueMap.get("Y"));
                        ps.setString(3, blueMap.get("Z"));
                        for (int j=0; j<indexRSSI.length; j++) {
                            ps.setString(j + 4, String.valueOf(offRssiArray[j][i]));
                        }
                        ps.setString(24, electricity[i]);
                        ps.setString(25, BluetoothUtil.moduleMap.get("startTime"));
                        ps.setString(26, blueMap.get("localMAC"));
                        ps.setString(27, blueMap.get("product"));
                        ps.setString(28, blueMap.get("version"));
                        ps.setString(29, BluetoothUtil.moduleMap.get("scanDuration"));
                        ps.setString(30, blueMap.get("periodicAdvertisingInterval"));
                        ps.setString(31, blueMap.get("txPower"));
                        ps.addBatch();

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    // 提交事务，并发送message更新UI
                    int[] ints = ps.executeBatch();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Message message = new Message();
                            if (ints.length > 0){
                                message.what = 3;
                            } else {
                                message.what = 4;
                            }
                            mHandler.sendMessage(message);
                        }
                    }).start();
                    conn.commit();
                    ps.clearBatch();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                jdbcUtil.close();
            }
        }).start();

    }

    /**
     * 通过message传过来的消息队列，指定更新UI
     */
    public MyHandler mHandler = new MyHandler(Looper.myLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    if (content) {
                        tv_content.append("\n" + blueMap.get("X") + " " + blueMap.get("Y") + " " + blueMap.get("Z") + "\n");
                        tv_content.append("Start scanning！" + "\n");
                        content = false;
                    } else {
                        tv_content.setText(blueMap.get("X") + " " + blueMap.get("Y") + " " + blueMap.get("Z") + "\n");
                        tv_content.append("Start scanning！" + "\n");
                    }
                    break;
                case 2:
                    offRssiArray = BluetoothUtil.rssiArray;
                    indexRSSI = BluetoothUtil.rssiIndex;
                    offElectricityArray = BluetoothUtil.electricityArray;
                    electricity = myUtil.getElectricity(indexRSSI,offElectricityArray);
                    tv_content.append("duration：" + blueMap.get("duration") + "s" + "\n");
                    tv_content.append("startTime：" + blueMap.get("startTime") + "\n");
                    tv_content.append("scanDuration：" + blueMap.get("scanDuration") + "ms" + "\n");
                    tv_content.append("deviceName：" + blueMap.get("deviceName") + "\n");
                    System.out.println("扫描结束！");
                    insert();
                    break;
                case 3:
                    tv_content.append("Successfully write to mysql！" + "\n");
                    break;
                case 4:
                    tv_content.append("Failed to write to mysql！" + "\n");
                    break;
                case 5:
                    tv_content.append("有数据为空！" + "\n");
                    break;
                default:
                    break;
            }
        }
    };
}