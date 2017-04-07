package com.liucong.mcgrady;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by liucong on 2017/4/7.
 * 缓存线程 处理缓存队列
 */

public class CacheDispatcher extends Thread {

    private static final String TAG = "CacheDispatcher";

    //缓存队列
    private LinkedBlockingQueue<ImageRequest> mCacheQueue;
    //网络队列
    private LinkedBlockingQueue<ImageRequest> mNetworkQueue;
    //缓存
    private Cache mCache;

    private boolean stop = false;

    public CacheDispatcher(LinkedBlockingQueue<ImageRequest> mCacheQueue, LinkedBlockingQueue<ImageRequest> mNetworkQueue, Cache mCache) {
        super("MyGrady #CacheDispatcher");
        this.mCacheQueue = mCacheQueue;
        this.mNetworkQueue = mNetworkQueue;
        this.mCache = mCache;
    }

    @Override
    public void run() {
        super.run();
        while(true){
            try {
                //从缓存队列中拿到请求
                Log.i(TAG, "阻塞中");
                ImageRequest take = mCacheQueue.take();
                Log.i(TAG, "已拿到请求");

                //判断请求的图片是否已经缓存过
                Bitmap bitmap = mCache.fromCache(take.getUrl());

                //缓存中不存在 就将请求转发给网络线程
                if(bitmap == null){
                    Log.i(TAG, "无缓存 将请求添加到网络队列中");
                    mNetworkQueue.put(take);
                    continue;
                }

                //将读取的结果转发出去
                Log.i(TAG, "有缓存 将图片设置到iv中");
                take.deliveryResult(bitmap);

            } catch (InterruptedException e) {
                if(stop){
                    return;
                }
                continue;
            }
        }
    }

    public void stopDispatcher(){
        stop = true;
        //停止当前线程
        interrupt();
    }

    public boolean isCanceled(){
        return stop;
    }
}
