package com.liucong.mcgrady;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;

import com.liucong.mcgrady.utils.MD5Utils;
import com.liucong.mcgrady.utils.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by liucong on 2017/4/7.
 * 磁盘缓存
 */

public class Cache {

    private static final String TAG = "McGrady #Cache";

    private String CACHE_PATH;
    private int mStrategy;
    private Context mContext;


    //缓存策略
    public static class DiskCacheStrategy{
        public static final int NONE = 1<<1;
        public static final int CACHE = 1<<2;
        public static final int DISK = 1<<3;
    }

    //最大缓存容量设置为程序最大可用内存的1/8
    public static int maxCacheCapacity = (int) (Runtime.getRuntime().maxMemory()/8);


    //LRU缓存
    public static LruCache<String,Bitmap> mMemoryCache = new LruCache<String,Bitmap>(maxCacheCapacity){
        @Override
        protected int sizeOf(String key, Bitmap value) {
            Log.i(TAG,"已缓存:"+(mMemoryCache.size()*100/mMemoryCache.maxSize()+"%"));
            return value.getByteCount();
        }
    };

    public Cache(int mStrategy, Context mContext) {
        this.mStrategy = mStrategy;
        this.mContext = mContext;
        CACHE_PATH = mContext.getCacheDir().getAbsolutePath();
    }

    public void setCacheStrategy(int strategy){
        this.mStrategy = strategy;
    }

    /**
     * 将图片保存到磁盘中
     * @param key
     * @param value
     */
    private void cacheToDisk(String key,Bitmap value) {
        String fileName = MD5Utils.encode(key);
        String path = mContext.getCacheDir().getAbsolutePath();
        if(value != null){
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
     * 缓存bitmap
     * @param url
     * @param bitmap
     */
    public void toCache(String url,Bitmap bitmap){
        //允许内存缓存
        if((mStrategy& DiskCacheStrategy.CACHE)!=0){
            //将数据缓存到lru中
            mMemoryCache.put(url,bitmap);
            Log.i(TAG,"Bitmap已缓存到Lru中");
        }

        //允许磁盘缓存
        if((mStrategy& DiskCacheStrategy.DISK)!=0){
            //将数据缓存到磁盘上
            cacheToDisk(url,bitmap);
            Log.i(TAG,"Bitmap已缓存到磁盘上");

        }
    }

    /**
     * 从缓存中获取图片
     * @param url
     * @return
     */
    public Bitmap fromCache(String url) {

        //允许内存缓存
        if((mStrategy& DiskCacheStrategy.CACHE)!=0){

            //从lru缓存中获取
            Bitmap cache = mMemoryCache.get(url);
            if(cache!=null){
                Log.i(TAG,"从cache中加载 加载成功");
                return cache;
            }
        }
        //允许磁盘缓存
        if((mStrategy & DiskCacheStrategy.DISK)!=0){
            String filePath = mContext.getCacheDir().getAbsolutePath()+"/"+ MD5Utils.encode(url);
            File file = new File(filePath);
            if(file.exists()){
                FileInputStream fis = null;
                try {
                    fis =  new FileInputStream(file);
                    Log.i(TAG,"从本地文件中加载");
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

    /**
     * 清理所有缓存
     */
    public void clear(){
        //清理Lru
        mMemoryCache.evictAll();
        Log.i(TAG, "内存缓存清理完毕");

        //清理磁盘
        if(!TextUtils.isEmpty(CACHE_PATH)){
            File file = new File(CACHE_PATH);
            if(!file.exists()){
                return;
            }

            if (file.isDirectory()){
                File[] files = file.listFiles();
                for(int i=0;i<files.length;i++){
                    files[i].delete();
                }
                Log.i(TAG, "磁盘缓存清理完毕");
            }
        }
    }

}
