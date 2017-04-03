package com.liucong.mcgrady;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by liucong on 2017/3/21.
 * 一个简单的图片加载框架
 * 使用lru缓存
 */

public class McGrady {

    private static McGrady mMcGrady;
    private static Context mContext;

    private static int cacheStrategy;
    private static ImageView outsideImageView;

    private static int defaultImgId ;
    private static int errorImgId ;

    //缓存策略
    public static class DiskCacheStrategy{
        public static final int NONE = 1<<1;
        public static final int CACHE = 1<<2;
        public static final int DISK = 1<<3;
    }

    //私有构造
    private McGrady(){}

    //最大缓存容量设置为程序最大可用内存的1/8
    public static int maxCacheCapacity = (int) (Runtime.getRuntime().maxMemory()/8);


    //LRU缓存
    public static LruCache<String,Bitmap> mMemoryCache = new LruCache<String,Bitmap>(maxCacheCapacity){
        @Override
        protected int sizeOf(String key, Bitmap value) {
            Log.i("McGrady","已缓存:"+(mMemoryCache.size()*100/mMemoryCache.maxSize()+"%"));
            return value.getByteCount();
        }
    };


    /**
     * 将图片保存到磁盘中
     * @param key
     * @param value
     */
    private static void cacheToDisk(String key,Bitmap value) {
        String fileName = MD5Utils.encode(key);
        String path = mContext.getCacheDir().getAbsolutePath();
        if(value != null){
            File cache = new File(path+"/"+fileName);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(path+"/"+fileName);
                value.compress(Bitmap.CompressFormat.JPEG,80,fos);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                StreamUtils.close(fos);
            }
        }
    }

    /**
     * 传入context
     * @param context
     * @return
     */
    public static McGrady from(Context context){
        mContext = context;

        if(mMcGrady == null){

            synchronized (McGrady.class){
                if( mMcGrady == null){
                    mMcGrady = new McGrady();
                }
            }
        }
        return mMcGrady;
    }

    /**
     * 加载图片
     * @param url
     * @param iv
     * @return
     */
    public static McGrady load(String url,ImageView iv){

        if(url==null || iv == null){
            return mMcGrady;
        }
        if(defaultImgId != 0){
            iv.setImageResource(defaultImgId);
        }

        Bitmap cache =null;

        // 首先查看缓存策略 是否允许缓存
        if((cacheStrategy&DiskCacheStrategy.NONE)==0){
            //拥有缓存
            //从缓存中读取数据
            cache = fromCache(url);
        }

        if(cache !=null){
            iv.setImageBitmap(cache);

            return mMcGrady;
        }

        //从网络中下载
        iv.setTag(url);
        new DownloadTask(iv).execute(url);
        return mMcGrady;
    }

    /**
     * 设置默认占位符
     * 在图片还未加载完成时设置显示的图片
     * @param imgResId
     * @return
     */
    public static McGrady placeHolder(int imgResId){
        defaultImgId = imgResId;
        return mMcGrady;
    }

    /**
     * 设置缓存策略
     * @param CacheStrategy
     * @return
     */
    public static McGrady diskCacheStrategy(int CacheStrategy){
        cacheStrategy = CacheStrategy;
        return mMcGrady;
    }

    public static McGrady error(int errImgResId){
        errorImgId = errImgResId;
        return  mMcGrady;
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

            if(iv==null){
                return;
            }

            if(bitmap == null) {
                if(errorImgId != 0){
                    iv.setImageResource(errorImgId);
                }

                return;
            }else{

                //iv 和bitmap不都为0
                //根据缓存策略进行操作

                //允许内存缓存
                if((cacheStrategy&DiskCacheStrategy.CACHE)!=0){
                    //将数据缓存到lru中
                    mMemoryCache.put(url,bitmap);
                    Log.i("McGrady","Bitmap已缓存到Lru中");
                }

                //允许磁盘缓存
                if((cacheStrategy&DiskCacheStrategy.DISK)!=0){
                    //将数据缓存到磁盘上
                    cacheToDisk(url,bitmap);
                    Log.i("McGrady","Bitmap已缓存到磁盘上");

                }

                String tag = (String) iv.getTag();
                if(tag != null && tag.equals(url)){
                    iv.setImageBitmap(bitmap);
                    //保存到cache
                    Log.i("McGrady","从网络中下载");
                }
            }
        }
    }


    /**
     * 从缓存中获取图片
     * @param url
     * @return
     */
    private static Bitmap fromCache(String url) {

        //能进这个方法说明至少允许使用一种缓存

        //允许内存缓存
        if((cacheStrategy&DiskCacheStrategy.CACHE)!=0){

            //从lru缓存中获取
            Bitmap cache = mMemoryCache.get(url);
            if(cache!=null){
                Log.i("McGrady","从cache中加载 加载成功");
                return cache;
            }
        }
        //允许磁盘缓存
        if((cacheStrategy&DiskCacheStrategy.DISK)!=0){
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
        }
        //如果没有缓存就返回null
        return null;
    }
}
