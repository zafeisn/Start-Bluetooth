package online.done.sea.location;
/**
 * 实现在线定位功能
 * 1、在线获取信号强度
 * 2、请求模型处理，返回位置结果
 * 3、地图定位显示
 */

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.Arrays;
import java.util.List;
import online.done.sea.R;
import online.done.sea.util.BluetoothUtil;
import online.done.sea.util.MyHandler;
import online.done.sea.util.OkHttpUtil;

public class LocationActivity extends AppCompatActivity {

    LinearLayout linear;
    TextView tv_location_x,tv_location_y;

    String startTime = BluetoothUtil.startTime;

    /**
     * 数据大小、中间变量、RSSI数组、模型输入input
     * 位置坐标、请求地址
     */
    private int [][] onlineRSSI = null;
    float[][][] input = new float[1][1][8];
    public float mLocation[] = new float[2];

//    private static String url = "http://119.29.179.102:8501/v1/models/xy_model:predict";
    private static String url = "http://175.178.104.65:8501/v1/models/xy_model:predict";

    JSONObject jsonObject = new JSONObject();
    OkHttpUtil okHttpUtil = new OkHttpUtil();
    LocationData location = new LocationData();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_location);

        onlineRSSI = BluetoothUtil.rssiArray;
        System.out.println(Arrays.deepToString(onlineRSSI));

        // 准备输入数据维度
        input = location.inputInit(onlineRSSI, 8);
        System.out.println(Arrays.deepToString(input));

        linear = findViewById(R.id.linear);
        tv_location_x = findViewById(R.id.tv_location_x);
        tv_location_y = findViewById(R.id.tv_location_y);


        jsonObject.put("instances", input);
        System.out.println("发送请求数据为：" + jsonObject);

        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = okHttpUtil.sendPost(url, jsonObject);
                JSONObject json = JSONObject.parseObject(result);
                JSONArray predictions = json.getJSONArray("predictions");
                JSONArray jsonArray = predictions.getJSONArray(0);
                List<Float> pre = jsonArray.toJavaList(Float.class);
                System.out.println("预测结果为：" + pre.get(0));
                /*mLocation[0] = pre.get(0);
                mLocation[1] = pre.get(1);*/
                mLocation[0] = isScaleOut(pre.get(0), 800f);
                mLocation[1] = isScaleOut(pre.get(1), 1200f);

                Message message = new Message();
                message.what = 1;
                myHandler.sendMessage(message);
            }
        }).start();
    }

    public Float isScaleOut(Float value, Float boundary) {
        if (value > boundary) {
            value = boundary;
        } else if (value < 0) {
            value = 0f;
        }
        return value;
    }

    /**
     * 更新事务
     */
    private MyHandler myHandler = new MyHandler(Looper.myLooper()){
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    // 2023/2/7已测试成功
                    showCenterView(linear,mLocation);
                    break;
                case 2:
                    System.out.println("****开始写入！****");
                    System.out.println(Arrays.toString(mLocation));
                    location.insertLocation(mLocation);
                    break;
            }
        }
    };

    /**
     * 地图显示，显示完成后写入数据库
     * @param view
     * @param location
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void showCenterView(View view, float[] location) {
        System.out.println(mLocation[0]);
        // 显示右下角坐标
        tv_location_x.setText(String.valueOf(mLocation[0]));
        tv_location_y.setText(String.valueOf(mLocation[1]));

        location[0] = location[0] * 5 / 4.2f;
        location[1] = location[1] * 10 / 7;

        // 标记
        FloatingManager.Builder builder = FloatingManager.getBuilder();
        builder.setAnchorView(view);
        FloatingManager manager = builder.build();
        manager.showCenterView(location,myHandler);
    }

}