package online.done.sea.dao;

/**
 * 用户表
 * 1、用户名
 * 2、密码
 * 3、备注信息
 */
public class User {
    private String username;
    private String password;
    private String des;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", des='" + des + '\'' +
                '}';
    }
}
