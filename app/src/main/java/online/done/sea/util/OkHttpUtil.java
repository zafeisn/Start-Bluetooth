package online.done.sea.util;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHttpUtil {

    String result = "";
    MediaType mediaType = MediaType.parse("application/json;charset=UTF-8");

    /**
     * 发送post请求
     * @param url
     * @param json
     * @return
     */
    public String sendPost(String url, JSONObject json) {

        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(mediaType, String.valueOf(json));
        Request request = new Request.Builder()
                .post(requestBody)
                .url(url)
                .build();
        try {
            Response response = okHttpClient.newCall(request).execute();
            System.out.println("response " + response);
            result = response.body().string();
            System.out.println("result " + result);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

}
