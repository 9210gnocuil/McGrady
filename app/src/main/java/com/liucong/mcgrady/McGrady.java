package com.liucong.mcgrady;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import com.liucong.mcgrady.utils.MD5Utils;
import com.liucong.mcgrady.utils.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by liucong on 2017/3/21.
 * 一个简单的图片加载框架
 * 使用lru缓存
 */

public class McGrady {

    private static final String TAG = "McGrady";

    private static McGrady mMcGrady;
    private static Context mContext;

    private static Thread loopThread;
    private static TaskLooper looper;

    //拿到当前设备上面的cpu的核心数
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    //核心线程池的大小为cpu的核心数+1
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    //最大线程池的大小为cpu的核心数*2+1
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    //超过核心线程池的线程空闲时存活时间
    private static final int KEEP_ALIVE = 1;

    //线程工厂
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "McGrady #" + mCount.getAndIncrement());
        }
    };

    //阻塞队列 用于保存任务
    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(128);

    //线程池
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAXIMUM_POOL_SIZE,
            KEEP_ALIVE,
            TimeUnit.SECONDS,
            sPoolWorkQueue,
            sThreadFactory
    );

    private static int cacheStrategy;

    private static int defaultImgId ;
    private static int errorImgId ;

    //缓存策略
    public static class DiskCacheStrategy{
        public static final int NONE = 1<<1;
        public static final int CACHE = 1<<2;
        public static final int DISK = 1<<3;
    }

    //私有构造
    private McGrady(){
        loopThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //在这个子线程中 开启轮询
                TaskLooper.prepare();
                looper = TaskLooper.myLooper();
                TaskLooper.loop();

            }
        });
        loopThread.start();
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

    /**
     * 处理消息
     */
    public static Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Task task = (Task) msg.obj;
            Bitmap bitmap = task.getBitmap();
            if(bitmap == null){
                task.getIv().setImageResource(R.drawable.error);
                return;
            }

            //缓存下载的图片
            McGrady.toCache(task.getUrl(), bitmap);

            //检查标记 以免图片错乱
            if (task.getUrl().equals(task.getIv().getTag()))
                task.getIv().setImageBitmap(bitmap);

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
    public static McGrady load(final String url, final ImageView iv){

        if(url==null || iv == null){
            throw new IllegalArgumentException("图片url或者ImageView不能为空!");
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

        try {
            //创建Runnable对象提交任务
            DownloadTask downTask = new DownloadTask(url,iv);

            Task task = new Task();
            task.setUrl(url);
            task.setIv(iv);
            task.setHandler(handler);
            task.setIv(iv);
            task.setCallable(downTask);
            task.setExecutor(EXECUTOR);

            //将消息提交到队列中 但是put是阻塞方法 不过这个队列默认是无限大
            //如果有重复的就不提交
            //空指针
            if(looper.mQueue.contains(task)){
                //队列中已存在该任务
                Log.i(TAG,"队列中已存在该任务");
                return mMcGrady;
            }

            looper.mQueue.put(task);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return mMcGrady;
    }

    private static class DownloadTask implements Callable<Bitmap>{

        private String imgUrl;
        private ImageView imageView;

        public DownloadTask(String url, ImageView iv) {
            imgUrl= url;
            imageView = iv;
        }

        @Override
        public Bitmap call() throws Exception {

            Log.i(TAG, "线程:"+Thread.currentThread().getName()+"正在下载");

            URL fileUrl = new URL(imgUrl);
            HttpURLConnection conn = (HttpURLConnection) fileUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            if(conn.getResponseCode()==200){
                InputStream inputStream = conn.getInputStream();
                Log.i(TAG, "线程:"+Thread.currentThread().getName()+"下载完毕");
                return BitmapFactory.decodeStream(inputStream);
            }

            return null;
        }
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

    /**
     * 缓存bitmap
     * @param url
     * @param bitmap
     */
    public static void toCache(String url,Bitmap bitmap){
        //允许内存缓存
        if((cacheStrategy&DiskCacheStrategy.CACHE)!=0){
            //将数据缓存到lru中
            mMemoryCache.put(url,bitmap);
            Log.i(TAG,"Bitmap已缓存到Lru中");
        }

        //允许磁盘缓存
        if((cacheStrategy&DiskCacheStrategy.DISK)!=0){
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
    private static Bitmap fromCache(String url) {

        //能进这个方法说明至少允许使用一种缓存

        //允许内存缓存
        if((cacheStrategy&DiskCacheStrategy.CACHE)!=0){

            //从lru缓存中获取
            Bitmap cache = mMemoryCache.get(url);
            if(cache!=null){
                Log.i(TAG,"从cache中加载 加载成功");
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
     * 关闭线程池
     * 在Activity的onDestroy中
     */
    public static void shutdownThreadPool(){
        looper.mQueue.clear();
        EXECUTOR.getQueue().clear();
        EXECUTOR.shutdownNow();

    }
}
