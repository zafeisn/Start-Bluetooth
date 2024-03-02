package online.done.sea.util;

/**
 * 1、初始化信标信号强度值、物理地址、以及RSSI存储顺序
 * 2、开启和关闭蓝牙功能
 * 3、定义开始和停止扫描方法
 * 4、添加扫描设备信息过滤方法以及获取设备信息
 * 5、利用异步扫描，同时添加扫描时长控制
 */

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BluetoothUtil extends Application {

    /**
     * 蓝牙信息存储
     * 1、用于处理message消息（判断是离线操作还是在线定位）
     * 2、记录RSSI值
     * 3-4、记录信标MAC地址
     * 5、RSSI存储排序
     * 6、本机地址
     * 7、记录
     */
    boolean isXY = false;
    public static int [][] rssiArray = null;
    public static int [][] electricityArray = null;
    List<String> macList = null;
//    String[] macArr = {"EB:18:CF:FD:9A:DD","D2:E3:21:8A:98:01","C6:2C:5F:F4:61:77",
//            "F3:03:67:AC:54:75", "D2:FB:8B:71:B7:47", "DA:18:72:C5:BC:72",
//            "D1:77:AB:8A:5F:EA","EE:72:02:75:60:C2","D6:34:54:6A:77:73","E1:65:64:DB:86:66",
//    "D6:2D:D3:1F:54:BA","D4:7B:6C:73:94:4D","C9:86:B2:07:E0:22","D6:60:BE:C6:D8:7D","C1:F5:0A:DC:72:EF",
//    "C5:86:91:96:66:95","FE:82:71:E0:6B:49","C9:46:7A:CC:D5:3B","F8:AB:FA:94:5E:16","F1:01:FC:F8:62:45"};
    String[] macArr = {
        "D6:2D:D3:1F:54:BA","D4:7B:6C:73:94:4D","C9:86:B2:07:E0:22","D6:60:BE:C6:D8:7D","C1:F5:0A:DC:72:EF",
        "C5:86:91:96:66:95","FE:82:71:E0:6B:49","C9:46:7A:CC:D5:3B","F8:AB:FA:94:5E:16","F1:01:FC:F8:62:45",
        "C6:2C:5F:F4:61:77","F3:03:67:AC:54:75","D2:FB:8B:71:B7:47","DA:18:72:C5:BC:72","D1:77:AB:8A:5F:EA",
        "EE:72:02:75:60:C2","D6:34:54:6A:77:73","E1:65:64:DB:86:66","EB:18:CF:FD:9A:DD","D2:E3:21:8A:98:01"
    };

    public static int [] rssiIndex;

    /**
     * 蓝牙类工具
     * 1、发射功率 6dBm
     * 2、广播间隔 1s
     * 3、扫描过滤器
     * 4、记录蓝牙信息集合
     * 5、蓝牙适配器
     * 6、扫描模式
     * 7、异步扫描
     * 8、蓝牙扫描器
     * 9、蓝牙管理器
     */
    List<ScanFilter> scanFilters = new ArrayList<>();
    public static Map<String, String> moduleMap = new HashMap<>();
    BluetoothAdapter bluetoothAdapter;
    ScanSettings scanSettings;
    Handler scanHandler;
    BluetoothLeScanner bluetoothLeScanner;
    BluetoothManager bluetoothManager;
    String localMAC = null;

    /**
     * 设备信息
     */
    String product = Build.MANUFACTURER.toLowerCase();  // 记录设备的制造商
    String version = Build.VERSION.RELEASE;  // 记录设备的系统版本

    /**
     * 解析时间
     * 1、记录每次的第一次扫描时间的次数，因为只记录开始时间
     * 2、记录每一次的第一次扫描时间
     * 3、扫描结束时间（默认10秒）
     * 4、开始扫描时间
     * 5、记录扫描时长
     */
    int record = 0;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static String startTime = null;
    long endTime;
    long startTimes;
    long scanDuration;

    /**
     * 消息队列
     */
    MyUtil myUtil;
    MyHandler messageHandler;


    /**
     * 初始化数组，同时设置RSSI初始值（未采集到的信标数值为-110）
     * 信标个数（目前初步定为20）
     * 每一次采集的数据最大个数（目前初步定为80）
     * 未采集到的信标数值为-110
     * 新增电池电量
     */
    public void initRSSI(){
        int nums = macArr.length;
        // 添加最大数据库个数
        int max = 200;
        int rssi = -110;
        int electricity = 0;
        rssiArray = new int[nums][max];
        electricityArray = new int[nums][max];
        macList = new ArrayList<>();
        rssiIndex = new int[nums];
        for (int i=0; i<nums; i++){
            for (int j=0; j<max; j++){
                rssiArray[i][j] = rssi;
                electricityArray[i][j] = electricity;
            }
        }
        for (int k=0; k<nums; k++){
            macList.add(macArr[k]);
        }
    }

    /**
     * 初始化蓝牙相关组件
     * 适配器用于操作蓝牙开启关闭
     * 蓝牙管理器用于获取蓝牙扫描器
     * 蓝牙设备名称等扫描信息
     * （离线阶段）默认扫描时长为30s
     * （定位阶段）默认扫描时长为30s
     *
     */
    public void initBLE(){
        scanHandler = new Handler(Looper.myLooper());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        localMAC = bluetoothAdapter.getAddress();
        moduleMap.put("localMAC",localMAC);
        moduleMap.put("deviceName", "OneStart");
        moduleMap.put("duration", "30");
        moduleMap.put("online", "30");
        moduleMap.put("product",product);
        moduleMap.put("version",version);
    }

    /**
     * 1、打开蓝牙
     * 使用弹窗提示用户是否同意打开蓝牙功能（BluetoothAdapter.ACTION_REQUEST_ENABLE）
     */
    public void isEnableBLE(FragmentActivity activity){
        int requestCode = 1;
        if (!bluetoothAdapter.isEnabled()){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(intent, requestCode);
            Log.i("打开蓝牙","成功打开！");
        } else {
            Toast.makeText(activity,"该设备已开启蓝牙~",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 2、关闭蓝牙
     */
    public void disEnableBLE(FragmentActivity activity){
        if (bluetoothAdapter.isEnabled()){
            bluetoothAdapter.disable();
            Log.i("关闭蓝牙","成功关闭！");
        } else {
            Toast.makeText(activity,"该设备还没有开启蓝牙哦~",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 3.1、开始扫描 = 定位
     * （开启新线程）
     * 先初始化，然后再判断适配器是否打开（蓝牙是否开启）
     * 同时设置扫描模式为低功耗，最后开启线程进入扫描
     */
    public void startScanning(BluetoothManager manager, FragmentActivity activity, MyUtil myUtil){
        this.myUtil = myUtil;
        this.myUtil.initSound(activity);
        messageHandler = this.myUtil.getHandler();
        initRSSI();
        bluetoothManager = manager;
        if (bluetoothAdapter.isEnabled()){
            record = 0;
            ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
            scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
            scanSettings = scanSettingsBuilder.build();
            scanHandler.post(new MyRunnable());
        } else {
            Toast.makeText(activity,"请打开蓝牙，否则是不能扫描的哦~",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 3.2、开始扫描 = 采集
     * （开启新线程）
     * 先初始化，然后再判断适配器是否打开（蓝牙是否开启）
     * 同时再判断是否有xyz输入，若有输入则设置扫描模式为低功耗
     * 最后开启线程进入扫描
     */
    public void startScanning(BluetoothManager manager, Boolean xyz, FragmentActivity activity, MyUtil myUtil){
        this.myUtil = myUtil;
        this.myUtil.initSound(activity);
        messageHandler = this.myUtil.getHandler();
        isXY = xyz;
        initRSSI();
        bluetoothManager = manager;
        if (bluetoothAdapter.isEnabled()){
            record = 0;
            if (isXY) {
                ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
                scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
                scanSettings = scanSettingsBuilder.build();
                scanHandler.post(new MyRunnable());
            }
        } else {
            Toast.makeText(activity,"请打开蓝牙，否则是不能扫描的哦~",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 4、添加新线程
     * 扫描到数据后，添加暂停并将数据写入到数据库中进行存储
     * 需要先获取蓝牙扫描器，然后再设置过滤条件、扫描模式以及回调进行扫描
     * 同时，如果是离线采集数据时，为了及时更新UI显示，通过线程向外发送message.what=1，进行更新处理
     * 若是在线定位用，则不需要发送message
     * 记录扫描时长，并在回调接口中做判断是否结束扫描
     */
    class MyRunnable implements Runnable{

        @Override
        public void run() {
            Log.i("test", "我走到run这里了");
            bluetoothLeScanner = bluetoothManager.getAdapter().getBluetoothLeScanner();
            bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // 使用线程向外发送message，进行事务处理（表示扫描开始）
                    if (isXY){
                        Message message = new Message();
                        message.what = 1;
                        messageHandler.sendMessage(message);
                        System.out.println("开始扫描了！");
                    }
                }
            }).start();
            if (isXY) {
                endTime = System.currentTimeMillis() + Integer.parseInt(moduleMap.get("duration")) * 1000;
            } else {
                endTime = System.currentTimeMillis() + Integer.parseInt(moduleMap.get("online")) * 1000;
            }
        }
    }

    /**
     * 利用回调接口（时间）停止/开始扫描，并记录扫描时长
     * 1、若有输入，则发送message.what=2
     * 2、若没有输入，则表示为定位阶段，发送message.what=5
     */
    public ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            getMyDevice(result);
            if (System.currentTimeMillis() > endTime) {
                bluetoothLeScanner.stopScan(scanCallback);
                scanDuration = System.currentTimeMillis() - startTimes;
                moduleMap.put("scanDuration", String.valueOf(scanDuration));
                // 播放结束音频
                if (isXY) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
//                            myUtil.playSound();
                            Message message = new Message();
                            message.what = 2;
                            messageHandler.sendMessage(message);
                        }
                    });
                    thread.start();
                } else {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
//                            myUtil.playSound();
                            Message message = new Message();
                            message.what = 0;
                            messageHandler.sendMessage(message);
                        }
                    });
                    thread.start();
                }
            }
        }
    };

    /**
     * 5、扫描蓝牙设备信息
     * 判断是否为指定信标名称，若是则获取其物理地址
     * 并判断是否为指定MAC地址，若是则获取其RSSI值、最后进行空间存储（未写入数据库）
     * 同时获取其发射功率、广播间隔、以及第一次扫描时间
     */
    public void getMyDevice(ScanResult scanResult) {

        String MAC = scanResult.getDevice().getAddress();
        String strPower = null;  // 电量值
        int electricity = 0;
        String periodicAdvertisingInterval = null;  // 广播间隔
        int intInterval = 0;
        String txPower = null;   // 发射功率
        int intTxPower = 0;

        if (macList.indexOf(MAC) != -1) {
            byte[] scanRecord = scanResult.getScanRecord().getBytes();
            String string = StringUtil.bytesToHexString(scanRecord);
            System.out.println("总数据：" + string);
            if ("EB:18:CF:FD:9A:DD".equals(MAC) || "D2:E3:21:8A:98:01".equals(MAC)){
                strPower = string.substring(114, 116);
                periodicAdvertisingInterval = string.substring(110, 114);
                txPower = string.substring(108, 110);
            } else {
                strPower = string.substring(70, 72);
                periodicAdvertisingInterval = string.substring(84, 86);
                txPower = string.substring(86, 88);
            }
            electricity = Integer.parseInt(strPower, 16);
            intInterval = Integer.parseInt(periodicAdvertisingInterval, 16);
            intTxPower = Integer.parseInt(txPower, 16);

            System.out.println("--- 电池电量 = " + electricity);
            System.out.println("--- 广播间隔 = " + intInterval);
            System.out.println("--- 发射功率 = " + intTxPower);
            moduleMap.put("txPower", String.valueOf(intTxPower));
            moduleMap.put("periodicAdvertisingInterval", String.valueOf(intInterval));
            int rssi = scanResult.getRssi();
            int index = macList.indexOf(MAC);
            // rssiArray[信标位置][数据个数]
            // electricityArray[信标位置][数据个数]
            rssiArray[index][rssiIndex[index]] = rssi;
            electricityArray[index][rssiIndex[index]] = electricity;
            rssiIndex[index] = ++rssiIndex[index];

            if (record == 0) {
                long timestampNanos = scanResult.getTimestampNanos();
                startTimes = System.currentTimeMillis() - SystemClock.elapsedRealtime() + timestampNanos / 1000000;
                startTime = simpleDateFormat.format(startTimes);
                moduleMap.put("startTime",startTime);
                record = 1;
            }
            Log.i("总记录","deviceName：" + scanResult.getDevice().getName() + " address：" + MAC
                    + " RSSI：" + scanResult.getRssi());
        }
    }

}
