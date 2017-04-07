package com.liucong.mcgrady;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by liucong on 2017/4/7.
 */

public class HttpStack {

    public Bitmap performRequest(ImageRequest ir) throws IOException {
        String urlStr = ir.getUrl();
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        int responseCode = connection.getResponseCode();
        if(responseCode == 200){
            InputStream inputStream = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            connection.disconnect();
            inputStream.close();

            return bitmap;
        }
        return null;
    }
}
