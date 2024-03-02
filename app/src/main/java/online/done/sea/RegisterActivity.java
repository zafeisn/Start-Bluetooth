package online.done.sea;
/**
 * 用户注册
 *
 * 1、填写手机号获取验证码注册（初始密码为123456）
 * 2、注册成功后，写入MySQL数据库
 */

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mob.MobSDK;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;
import online.done.sea.util.JDBCUtil;

import static cn.smssdk.SMSSDK.*;
import static cn.smssdk.SMSSDK.EVENT_GET_VERIFICATION_CODE;

public class RegisterActivity extends AppCompatActivity {

    private EditText ed_register_phone,ed_register_code;
    private Button bt_register,bt_request_code;

    private String phone;
    private String code;


    int i = 30;  // 验证码倒计时
    boolean isCode = false;
    boolean temp = true;  // 注册失败标识符

    // 连接数据库
    JDBCUtil jdbcUtil = new JDBCUtil();
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_register);

        MobSDK.init(this);

        initView();
        setListener();
        // 获取验证码
        sendCode();
    }

    public void initView(){
        ed_register_phone = findViewById(R.id.ed_register_phone);
        ed_register_code = findViewById(R.id.ed_register_code);
        bt_register = findViewById(R.id.bt_register);
        bt_request_code = findViewById(R.id.bt_request_code);
    }

    public void setListener(){
        Onclick onclick = new Onclick();
        bt_register.setOnClickListener(onclick);
        bt_request_code.setOnClickListener(onclick);
    }

    class Onclick implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.bt_register:
                    // 注册成功，跳转登录页面
                    register();
                    // 注册失败，给出提示并刷新当前页面
                    break;
                case R.id.bt_request_code:
                    requestCode();
                    break;
            }
        }
    }

    // 注册
    public void register(){

        code = ed_register_code.getText().toString();
        phone = ed_register_phone.getText().toString();
        if (!temp){
            Toast.makeText(RegisterActivity.this,"该手机号已注册",Toast.LENGTH_SHORT).show();
        }
        if (isPhone(phone) && temp) {
            SMSSDK.submitVerificationCode("86", phone, code);
        }
    }

    // 二次注册判断
    public void isRegisted(){

        ed_register_phone.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus){
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // 失去焦点连接数据库进行判断
                            conn = jdbcUtil.getConn();
                            String sql = "select * from user where username=?";
                            try {
                                ps = conn.prepareStatement(sql);
                                ps.setString(1,phone);
                                rs = ps.executeQuery();
                                if (rs.next()){
                                    temp = false;
                                    System.out.println("temp = " + temp);
                                }
                                jdbcUtil.close();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.start();
                }
            }
        });
    }

    // 请求验证码
    public void sendCode(){
        EventHandler eh = new EventHandler(){
            @Override
            public void afterEvent(int event, int result, Object data) {
                // TODO 此处不可直接处理UI线程，处理后续操作需传到主线程中操作
                Message msg = new Message();
                msg.arg1 = event;
                msg.arg2 = result;
                msg.obj = data;
                handler.sendMessage(msg);

            }
        };
        //注册一个事件回调监听，用于处理SMSSDK接口请求的结果
        SMSSDK.registerEventHandler(eh);
    }

    // 判断注册是否为空
    public void requestCode(){

        phone = ed_register_phone.getText().toString();
        isRegisted();

        if (isPhone(phone)){
            SMSSDK.getVerificationCode("86",phone);
            bt_request_code.setClickable(false);
            bt_request_code.setText("重新发送（" + i + "）");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (; i>0; i--){
                        handler.sendEmptyMessage(-1);
                        if (i <= 0){
                            break;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    handler.sendEmptyMessage(-2);
                }

            }).start();
        }
    }

    // 获取信息(判断验证码倒计时)
    Handler handler = new Handler(Looper.myLooper()){
        public void handleMessage(Message msg){
            if (msg.what == -1){
                bt_request_code.setText("重新发送（" + i + "）");
            } else if (msg.what == -2){
                bt_request_code.setText("获取验证码");
                bt_request_code.setClickable(true);
                i = 30;
            } else if (msg.what == -3){
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // 写入数据库
                        conn = jdbcUtil.getConn();
                        String sql = "insert into user(username,password,des) values(?,?,?)";
                        try {
                            ps = conn.prepareStatement(sql);
                            ps.setString(1, phone);
                            ps.setString(2, "123456");
                            ps.setString(3, "null");
                            int i = ps.executeUpdate();
                            if (i > 0) {
                                Log.i("写入数据库", "成功！");
                            } else {
                                Log.i("写入数据库", "失败！");
                            }
                            conn.commit();
                            jdbcUtil.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();

            } else if (msg.what == -4){
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
                thread.start();
            } else {
                int event = msg.arg1;
                int result = msg.arg2;
                Object data = msg.obj;
                if (result == SMSSDK.RESULT_COMPLETE){
                    // 短信注册成功
                    if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE){
                        handler.sendEmptyMessage(-3);
                        Toast.makeText(getApplicationContext(), "注册成功", Toast.LENGTH_SHORT).show();
                        // 跳转
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                    } else if (event == EVENT_GET_VERIFICATION_CODE){
                        Toast.makeText(getApplicationContext(),"正在获取验证码",Toast.LENGTH_SHORT).show();
                    }
                } else {
                    isCode = false;
                    ((Throwable) data).printStackTrace();
                    String str = data.toString();
                    Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SMSSDK.unregisterAllEventHandler();
    }

    // 判断手机号是否为空并且判断手机号格式
    public boolean isPhone(String phone){
        String telRegex = "[1][358]\\d{9}";
        if ("".equals(phone)){
            Toast.makeText(this,"手机号不能为空",Toast.LENGTH_SHORT).show();
            return false;
        } else {
            if (phone.length() == 11){
                if (phone.matches(telRegex)){
                    return true;
                } else {
                    Toast.makeText(this,"手机号输入有误",Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else {
                Toast.makeText(this,"手机号输入有误",Toast.LENGTH_SHORT).show();
                return false;
            }
        }

    }

}