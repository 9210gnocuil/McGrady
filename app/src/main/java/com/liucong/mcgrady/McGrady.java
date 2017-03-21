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

    private static int cacheStrategy;
    private static ImageView outsideImageView;

    private static int defaultImgId ;
    private static int errorImgId ;

    //缓存策略
    public static class DiskCacheStrategy{
        public static final int NONE = 1;
        public static final int CACHE_ONLY = 2;
        public static final int DISK_ONLY = 3;
        public static final int DEFAULT = 0; //默认缓存内存和磁盘
    }

    //保存图片的仓库
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

    /**
     * 将图片保存到磁盘中
     * @param key
     * @param value
     */
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

    /**
     * 传入context
     * @param context
     * @return
     */
    public static McGrady from(Context context){
        mContext = context;

        if( mMcGrady == null){
            mMcGrady = new McGrady();
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

        //首先查看缓存策略
        Bitmap cache =null;
        switch (cacheStrategy){
            case DiskCacheStrategy.DEFAULT:
                // 首先检查是否存在缓存(内存，磁盘)，
                // 如果有缓存就从缓存中读取并将读取的这个引用放到集合最前面
                cache = fromCache(url,DiskCacheStrategy.DEFAULT);
                break;
            case DiskCacheStrategy.CACHE_ONLY:
                cache = fromCache(url,DiskCacheStrategy.CACHE_ONLY);
                break;
            case DiskCacheStrategy.DISK_ONLY:
                cache = fromCache(url,DiskCacheStrategy.DISK_ONLY);
                break;
            case DiskCacheStrategy.NONE:
                break;
        }


        if(cache !=null){
            iv.setImageBitmap(cache);
            if(cacheStrategy == DiskCacheStrategy.DEFAULT || cacheStrategy == DiskCacheStrategy.CACHE_ONLY)
                imageRepository.put(url,new SoftReference<Bitmap>(cache));
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
                //看模式
                SoftReference<Bitmap> ref = new SoftReference<Bitmap>(bitmap);

                switch (cacheStrategy){
                    case DiskCacheStrategy.DEFAULT:

                        cacheToDisk(url,ref);
                        imageRepository.put(url,ref);

                        break;
                    case DiskCacheStrategy.CACHE_ONLY:

                        imageRepository.put(url,ref);

                        break;
                    case DiskCacheStrategy.DISK_ONLY:
                        cacheToDisk(url,ref);

                        break;
                    case DiskCacheStrategy.NONE:
                        break;
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
    private static Bitmap fromCache(String url,int cacheMode) {

        //如果缓存模式为cache或者默认
        if(cacheMode == DiskCacheStrategy.CACHE_ONLY || cacheMode == DiskCacheStrategy.DEFAULT){
            //首先检查内存中是否存在
            SoftReference<Bitmap> bitmapRef = imageRepository.get(url);
            if(bitmapRef!=null){
                if(bitmapRef.get()!=null){
                    Log.i("McGrady","从cache中加载");
                    return bitmapRef.get();
                }
            }
        }

        //如果缓存模式为默认或者disk
        if(cacheMode==DiskCacheStrategy.DEFAULT || cacheMode == DiskCacheStrategy.DISK_ONLY){
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
        }
        return null;
    }
}
