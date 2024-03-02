package online.done.sea.util;

import android.media.SoundPool;
import androidx.fragment.app.FragmentActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;

import online.done.sea.bluetooth.BluetoothActivity;
import online.done.sea.R;

public class MyUtil {

    private MyHandler handler = null;
    SoundPool soundPool;
    int soundID;

    // set方法
    public void setHandler(MyHandler handler) {
        this.handler = handler;
    }

    // get方法
    public MyHandler getHandler() {
        return handler;
    }

    /**
     * 加载音频
     * @param activity
     */
    public void initSound(FragmentActivity activity){
        System.out.println("加载音频！");
        soundPool = new SoundPool.Builder().build();
        soundID = soundPool.load(activity,R.raw.tpp,1);
    }

    /**
     * 播放音频
     */
    public void playSound(){
        System.out.println("播放音频！");
        soundPool.play(soundID,0.1f,0.5f,0,0,1);
    }

    /**
     * 求数组的最大值
     * @param rssi
     * @return
     */
    public int getMax(int[] rssi){
        int max = rssi[0];
        for (int i=0; i<rssi.length; i++){
            if (rssi[i]>max){
                max = rssi[i];
            }
        }
        return max;
    }

    /**
     * 求数组中的最小值
     * @param rssi
     * @return
     */
    public int getMin(int[] rssi){
        int min = rssi[0];
        for (int i=0; i<rssi.length; i++){
            if (rssi[i]<min){
                min = rssi[i];
            }
        }
        return min;
    }

    /**
     * 获取电池电量
     * @param rssi
     * @param offElectricityArray
     * @return
     */
    public String[] getElectricity(int[] rssi, int[][] offElectricityArray){

        int max = getMax(rssi);
        int min = getMin(rssi);
        int count = (max + min) / 2;
        String[] electricityArray = new String[max];  // 得到电池电量数组
        // 添加写入数据库个数
        for (int i=0; i<max; i++) {
            String electricity = null;  // 电池电量
            for (int k = 0; k < offElectricityArray.length; k++) {
                int intElectricity = offElectricityArray[k][i];
                if (intElectricity == 0) {
                    if (electricity == null) {
                        electricity = "00";
                        electricity += "" + intElectricity;
                    } else {
                        electricity += "00" + intElectricity;
                    }
                } else if (intElectricity > 0 && intElectricity < 100) {
                    electricity += "0" + intElectricity;
                } else {
                    electricity += "" + intElectricity;
                }
            }
            electricityArray[i] = electricity;
        }
        return electricityArray;
    }

}
