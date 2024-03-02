package online.done.sea.server;
/**
 * 测试与服务器通信
 * 1、填写ip和端口号进行连接
 * 2、发送到服务器端
 * 3、接收服务器端的响应请求
 */

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import online.done.sea.R;

public class SocketServerActivity extends AppCompatActivity {

    Button bt_connect,bt_send,bt_receive;
    EditText ipText,portText,ed_send_msg,ed_receive_msg;
    TextView tv_conn_state,tv_send_state,tv_receive_state;

    String receiveMsg;  // 接收的消息回显
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /********* 连接服务器 *******/
    ConnServer server;
    /********* 连接服务器 *******/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_server);

        initComponent();
        setListener();
    }

    // 初始化
    public void initComponent(){
        bt_connect = findViewById(R.id.bt_connect);
        bt_send = findViewById(R.id.bt_send);
        bt_receive = findViewById(R.id.bt_receive);
        ipText = findViewById(R.id.ipText);
        portText = findViewById(R.id.portText);
        ed_send_msg = findViewById(R.id.ed_send_msg);
        ed_receive_msg = findViewById(R.id.ed_receive_msg);
        tv_conn_state = findViewById(R.id.tv_conn_state);
        tv_send_state = findViewById(R.id.tv_send_state);
        tv_receive_state = findViewById(R.id.tv_receive_state);
    }

    // 监听事件
    public void setListener(){
        Onclick onclick = new Onclick();
        bt_connect.setOnClickListener(onclick);
        bt_send.setOnClickListener(onclick);
        bt_receive.setOnClickListener(onclick);
    }

    // 创建点击事件
    class Onclick implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.bt_connect:
                    // 连接服务器
                    begin();
                    break;
                case R.id.bt_send:
                    // 发送
                    server.sendMsg(ed_send_msg.getText().toString());
                    break;
                case R.id.bt_receive:
                    // 接收
                    server.receiveMsg();
                    break;
            }
        }
    }

    // 准备连接
    public void begin(){
        String ip = ipText.getText().toString();
        int port = Integer.parseInt(portText.getText().toString());
        server = new ConnServer(ip, port);
        server.getSocketCon();
    }

    // 新线程
    class ConnServer extends Thread{
        private String ip;
        private int port;
        private InputStream inputStream;
        private OutputStream outputStream;

        public ConnServer(String ip, int port){
            this.ip = ip;
            this.port = port;
        }

        public void getSocketCon(){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Message message = new Message();
                    try {
                        // 进行socket通信
                        Socket socket = new Socket(ip, port);
                        // 获取输入输出流
                        inputStream = socket.getInputStream();
                        outputStream = socket.getOutputStream();
                        message.what = 1;  // 更新UI
                    } catch (IOException e) {
                        message.what = -1;  // 更新UI
                        e.printStackTrace();
                        return;  // 异常结束，跳出线程
                    }
                    handler.sendMessage(message);
                }
            }).start();
        }

        // 发送
        public void sendMsg(String Msg){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Message message = new Message();
                    String msg = Msg;
                    msg += "#";
                    try {
                        outputStream.write(msg.getBytes());
                        outputStream.flush();
                        message.what = 2;

                    } catch (IOException e) {
                        message.what = -2;
                        e.printStackTrace();
                    }
                    handler.sendMessage(message);
                }
            });
            thread.start();
        }

        // 接收
        public void receiveMsg(){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Message message = new Message();
                    // 线程安全
                    StringBuffer stringBuffer = new StringBuffer();
                    try {
                        int read = inputStream.read();
                        while (read != '#'){
                            stringBuffer.append((char)read);  // 存入
                            read = inputStream.read();  // 读取
                        }
                        String temp = stringBuffer.toString();
                        receiveMsg = new String(temp.getBytes("ISO-8859-1"),"utf-8");
                        message.what = 3;
                    } catch (IOException e) {
                        message.what = -3;
                        e.printStackTrace();
                    }
                    handler.sendMessage(message);
                }
            });
            thread.start();
        }

        private Handler handler = new Handler(Looper.myLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case 1:
                        tv_conn_state.setText("连接成功 " + simpleDateFormat.format(new Date()));
                        break;
                    case -1:
                        tv_conn_state.setText("连接失败 " + simpleDateFormat.format(new Date()));
                        break;
                    case 2:
                        tv_send_state.setText("发送成功 " + simpleDateFormat.format(new Date()));
                        break;
                    case -2:
                        tv_send_state.setText("发送失败 " + simpleDateFormat.format(new Date()));
                        break;
                    case 3:
                        tv_receive_state.setText("接收成功 " + simpleDateFormat.format(new Date()));
                        ed_receive_msg.setText(receiveMsg);
                        break;
                    case -3:
                        tv_receive_state.setText("接收失败 " + simpleDateFormat.format(new Date()));
                        break;
                }
            }
        };
    }
}