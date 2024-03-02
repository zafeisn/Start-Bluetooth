package online.done.sea;

/**
 * 登录页面
 * 1、填写账号密码进行登录（初始密码为123456）
 * 2、记住密码
 * 3、找回密码
 * 4、自动登录
 * 5、注册账号
 * 6、第三方账号登录
 */

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import online.done.sea.index.IndexActivity;
import online.done.sea.util.JDBCUtil;

public class LoginActivity extends AppCompatActivity {

    private EditText ed_login_name,ed_login_pwd;
    private Button bt_login;
    private TextView register_name;
    public static final String phone = "userName";

    String userName;
    String password;

    /*********** 数据库操作 *************/
    JDBCUtil jdbcUtil = new JDBCUtil();
    private Connection conn = null;
    private PreparedStatement ps = null;
    private ResultSet rs = null;
    /*********** 数据库操作 *************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_login);

        // 1、初始化控件
        myInit();
        // 2、监听事件
        setListener();

    }

    /**************** 初始化控件 *****************/
    private void myInit(){

        ed_login_name = findViewById(R.id.ed_login_name);
        ed_login_pwd = findViewById(R.id.ed_login_pwd);
        bt_login = findViewById(R.id.bt_login);
        register_name = findViewById(R.id.register_name);
    }

    public void setListener(){
        Onclick onclick = new Onclick();
        bt_login.setOnClickListener(onclick);
        register_name.setOnClickListener(onclick);
    }

    /**************** 点击事件 *****************/
    class Onclick implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.bt_login:
                    //登录
                    login();
                    break;
                case R.id.register_name:
                    // 注册
//                    regist();
                    break;
            }

        }
    }

    // 判断用户名和密码是否为空
    public boolean isNamePwd(){

        userName = ed_login_name.getText().toString();
        password = ed_login_pwd.getText().toString();
        if (userName == null || userName.length() == 0){
            Toast.makeText(this,"账号不能为空",Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password == null || password.length() == 0){
            Toast.makeText(this,"密码不能为空",Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;

    }

    /**************** 登录 *****************/
    public void login() {

        if (isNamePwd()) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    conn = jdbcUtil.getConn();
                    ps = jdbcUtil.selectUser(conn);
                    try {
                        ps.setString(1, userName);
                        ps.setString(2, password);
                        rs = ps.executeQuery();
                        if (rs.next()) {
                            // 登录成功
                            Intent intent = new Intent(LoginActivity.this, IndexActivity.class);
                            intent.putExtra(phone,userName);
                            startActivity(intent);
                        }else {
                            Looper.prepare();
                            Toast.makeText(LoginActivity.this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
                            Looper.loop();
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

    /**************** 注册 *****************/
    public void regist(){
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

}