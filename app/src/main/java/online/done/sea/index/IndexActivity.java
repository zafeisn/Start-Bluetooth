package online.done.sea.index;
/**
 * 首页
 * 1、蓝牙扫描
 * 2、服务器连接测试
 * 3、定位功能
 */

import android.app.AppComponentFactory;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import java.util.Timer;
import java.util.TimerTask;
import online.done.sea.bluetooth.BluetoothActivity;
import online.done.sea.LoginActivity;
import online.done.sea.R;
import online.done.sea.location.LocationActivity;
import online.done.sea.location.LocationData;
import online.done.sea.server.SocketServerActivity;
import online.done.sea.test.MapTestActivity;
import online.done.sea.util.MyHandler;

public class IndexActivity extends AppCompatActivity {

    LinearLayout ll_bluetooth,ll_socketServer,ll_location,ll_test;
    String phone;

    LocationData locationData = new LocationData();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_index);
        phone = getIntent().getStringExtra(LoginActivity.phone);

        // 初始化控件
        myInit();
        setListener();
    }

    /**************** 初始化控件 *****************/
    private void myInit(){
        ll_bluetooth = findViewById(R.id.ll_bluetooth);
        ll_socketServer = findViewById(R.id.ll_socketServer);
        ll_location = findViewById(R.id.ll_location);
        ll_test = findViewById(R.id.ll_test);
    }

    // 创建监听事件
    public void setListener(){
        Onclick onclick = new Onclick();
        ll_bluetooth.setOnClickListener(onclick);
        ll_socketServer.setOnClickListener(onclick);
        ll_location.setOnClickListener(onclick);
        ll_test.setOnClickListener(onclick);
    }

    // 创建点击事件
    class Onclick implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.ll_bluetooth:
                    // 跳转到蓝牙采集页面
                    Intent bluetooth = new Intent(IndexActivity.this, BluetoothActivity.class);
                    bluetooth.putExtra(LoginActivity.phone,phone);
                    startActivity(bluetooth);
                    break;
                case R.id.ll_socketServer:
                    // 跳转到服务器测试页面
                    Intent server = new Intent(IndexActivity.this, SocketServerActivity.class);
                    server.putExtra(LoginActivity.phone,phone);
                    startActivity(server);
                    break;
                case R.id.ll_location:
                    // 点击定位，获取蓝牙数据
                    getLocationData();
                    getTimerTask();
                    break;
                case R.id.ll_test:
                    Intent test = new Intent(IndexActivity.this, MapTestActivity.class);
                    startActivity(test);
                    break;
            }
        }
    }

    /**
     * 定位地图等待3秒时长
     */
    private void getTimerTask(){
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                // 2023/2/17已测试成功
                Intent location = new Intent(IndexActivity.this,LocationActivity.class);
                startActivity(location);
//                overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
            }
        };
        timer.schedule(task,35000);
    }

    /**
     * 在线定位获取数据
     */
    public void getLocationData(){
        
        Toast.makeText(IndexActivity.this,"定位中，请耐心等待~",Toast.LENGTH_LONG).show();
        locationData.startScan(IndexActivity.this,myHandler);
    }

    /**
     * 0：进行跳转
     */
    MyHandler myHandler = new MyHandler(Looper.myLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    locationData.insertOnline();
                    break;
            }
        }
    };

}