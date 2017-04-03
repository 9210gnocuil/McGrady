package com.liucong.mcgrady;

import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Created by liucong on 2017/4/3.
 */

public class Task {

    private static final String TAG = "Task";

    private String url;
    private ImageView iv;
    private Bitmap bitmap;
    private ExecutorService executor;
    private Handler handler;
    private Callable<Bitmap> callable;

    private static Task taskPool ;

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Callable<Bitmap> getCallable() {
        return callable;
    }

    public void setCallable(Callable<Bitmap> callable) {
        this.callable = callable;
    }

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ImageView getIv() {
        return iv;
    }

    public void setIv(ImageView iv) {
        this.iv = iv;
    }

    @Override
    public boolean equals(Object o) {
        if(o==null)
            return false;
        Task other = (Task) o;
        if(this.getIv().equals(other.getIv())){
            if(this.getUrl().equals(other.getUrl())){
                return true;
            }
        }
        return false;
    }
}
