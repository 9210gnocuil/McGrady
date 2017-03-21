package com.liucong.mcgrady;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.liucong.mcgrady.utils.MD5Utils;
import com.liucong.mcgrady.utils.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by liucong on 2017/3/21.
 * 一个简单的图片加载框架
 */

public class McGrady {

    private static McGrady mMcGrady;
    private static Context mContext;
    private static final int MAX_CAPACITY = 16;

    private static LinkedHashMap<String,SoftReference<Bitmap>> imageRepository = new LinkedHashMap<String,SoftReference<Bitmap>>(MAX_CAPACITY){
        @Override
        protected boolean removeEldestEntry(Entry<String, SoftReference<Bitmap>> eldest) {
            //首先检查是否到了容量上限
            if(this.size()>MAX_CAPACITY){
                //到了上限，删除最近最少使用的那个
                return true;
            }
            //王磁盘里面添加数据
            cacheToDisk(eldest.getKey(),eldest.getValue());
            return false;
        }
    };

    //将图片保存到磁盘中 应该以下载地址作MD5加密后作为文件名
    private static void cacheToDisk(String key,SoftReference<Bitmap> value) {
        String fileName = MD5Utils.encode(key);
        String path = mContext.getCacheDir().getAbsolutePath();
        if(value != null){
            Bitmap bitmap = value.get();
            if (bitmap !=null){
                File cache = new File(path+"/"+fileName);
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(path+"/"+fileName);
                    bitmap.compress(Bitmap.CompressFormat.JPEG,80,fos);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    StreamUtils.close(fos);
                }
            }
        }
    }

    public static McGrady from(Context context){
        mContext = context;

        if( mMcGrady == null){
            mMcGrady = new McGrady();
        }
        return mMcGrady;
    }

    public static McGrady load(String url,ImageView iv){

        if(url==null){
            return mMcGrady;
        }

        // 首先检查是否存在缓存(内存，磁盘)，
        // 如果有缓存就从缓存中读取并将读取的这个引用放到集合最前面
        Bitmap cache = fromCache(url);
        if(cache !=null){
            iv.setImageBitmap(cache);
            imageRepository.put(url,new SoftReference<Bitmap>(cache));
            return mMcGrady;
        }
        //从网络中下载
        new DownloadTask(iv).execute(url);
        return mMcGrady;
    }

    //下载图片的任务
    private static class DownloadTask extends AsyncTask<String,Void,Bitmap>{

        private String url;
        private ImageView iv;

        public DownloadTask(ImageView iv) {
            this.iv = iv;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            if(params==null) {
                return null;
            }
            try {
                url = params[0];
                URL fileUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) fileUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                if(conn.getResponseCode()==200){
                    InputStream inputStream = conn.getInputStream();
                    return BitmapFactory.decodeStream(inputStream);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        //下载完毕后将图片保存到缓存中并设置到对应的控件里面
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(bitmap == null)
                return;

            if(iv!=null){
                iv.setImageBitmap(bitmap);
            }
            //保存到cache
            Log.i("McGrady","从网络中下载");
            SoftReference<Bitmap> ref = new SoftReference<>(bitmap);
            cacheToDisk(url,ref);
            imageRepository.put(url,ref);
        }
    }


    /**
     * 从缓存中获取图片
     * @param url
     * @return
     */
    private static Bitmap fromCache(String url) {
        //首先检查内存中是否存在
        SoftReference<Bitmap> bitmapRef = imageRepository.get(url);
        if(bitmapRef!=null){
            if(bitmapRef.get()!=null){
                Log.i("McGrady","从cache中加载");
                return bitmapRef.get();
            }
        }
        //内存中不存在
        //检查磁盘
        String filePath = mContext.getCacheDir().getAbsolutePath()+"/"+MD5Utils.encode(url);
        File file = new File(filePath);
        if(file.exists()){
            FileInputStream fis = null;
            try {
                fis =  new FileInputStream(file);
                Log.i("McGrady","从本地文件中加载");
                return BitmapFactory.decodeStream(fis);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                StreamUtils.close(fis);
            }
        }
        return null;
    }
}
