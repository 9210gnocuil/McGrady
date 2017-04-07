package com.liucong.mcgrady;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import java.util.concurrent.LinkedBlockingQueue;


/**
 * Created by liucong on 2017/3/21.
 * 一个简单的图片加载框架
 * 使用lru缓存
 */

public class McGrady {

    private static final String TAG = "McGrady";

    private static McGrady sMcGrady;
    private static Context sContext;

    //缓存队列
    private static LinkedBlockingQueue<ImageRequest> mCacheQueue = new LinkedBlockingQueue<ImageRequest>();
    //网络队列
    private static LinkedBlockingQueue<ImageRequest> mNetworkQueue = new LinkedBlockingQueue<ImageRequest>();

    private int cacheStrategy;

    private int defaultImgId ;
    private int errorImgId ;


    private NetworkDispatcher mNetworkDispatcher;
    private CacheDispatcher mCacheDispatcher;
    private HttpStack mStack;
    private Cache mCache;


    //私有构造
    private McGrady(){

        mStack = new HttpStack();
        mCache = new Cache(cacheStrategy,sContext);

        mNetworkDispatcher = new NetworkDispatcher(mCache,mStack,mNetworkQueue);
        mCacheDispatcher = new CacheDispatcher(mCacheQueue,mNetworkQueue,mCache);

        mNetworkDispatcher.start();
        mCacheDispatcher.start();
    }

    /**
     * 处理消息
     * 保证在主线程中进行
     */
    public static final Handler sHandler = new Handler(Looper.getMainLooper());


    /**
     * 传入context
     * @param context
     * @return
     */
    public static McGrady from(Context context){
        sContext = context;

        if(sMcGrady == null){
            synchronized (McGrady.class){
                if( sMcGrady == null){
                    sMcGrady = new McGrady();
                }
            }
        }
        return sMcGrady;
    }

    /**
     * 加载图片
     * @param url
     * @param iv
     * @return
     */
    public McGrady load(final String url, final ImageView iv){

        //设置默认缓存
        if(defaultImgId!=0){
            setDefaultImage(iv);
        }

        //首先添加到缓存队列 如果缓存队列中检查到该请求并没有缓存 就会把请求添加到网络队列中
        iv.setTag(url);
        ImageRequest request = new ImageRequest(url,sHandler,iv,errorImgId);

        try {
            if(!mCacheQueue.contains(request)){
                Log.i(TAG, "已将任务添加到mCacheQueue "+"当前队列大小:"+mCacheQueue.size());
                mCacheQueue.put(request);
                return sMcGrady;
            }
            Log.i(TAG, "mCacheQueue已存在相同的任务，不需要多次添加");
        } catch (InterruptedException e) {
            if(mCacheDispatcher.isCanceled()){
                return sMcGrady;
            }
        }
        return sMcGrady;
    }

    private void setDefaultImage(final ImageView imageView) {
        if(Looper.getMainLooper().getThread() == Thread.currentThread()){
            //如果是主线程
            imageView.setImageResource(defaultImgId);
        }else{
            sHandler.post(new Runnable() {
                @Override
                public void run() {

                    imageView.setImageResource(defaultImgId);
                }
            });
        }
    }

    /**
     * 设置默认占位符
     * 在图片还未加载完成时设置显示的图片
     * @param imgResId
     * @return
     */
    public McGrady placeHolder(int imgResId){
        defaultImgId = imgResId;
        return sMcGrady;
    }

    /**
     * 设置缓存策略
     * @param CacheStrategy
     * @return
     */
    public McGrady diskCacheStrategy(int CacheStrategy){
        cacheStrategy = CacheStrategy;
        mCache.setCacheStrategy(cacheStrategy);
        return sMcGrady;
    }

    public McGrady error(int errImgResId){
        errorImgId = errImgResId;
        return sMcGrady;
    }

    public void shutdown(){
        if(mNetworkDispatcher!=null){
            mNetworkDispatcher.stopDispatcher();
        }
        if(mCacheDispatcher!=null){
            mCacheDispatcher.stopDispatcher();
        }
    }
}
