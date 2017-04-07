package com.liucong.mcgrady;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by liucong on 2017/4/7.
 */

public class NetworkDispatcher extends Thread {


    private static final String TAG = "NetworkDispatcher";

    private LinkedBlockingQueue<ImageRequest> mNetworkQueue;
    private ThreadPoolExecutor mExecutor;
    private HttpStack mStack;
    private Cache mCache;
    private boolean stop = false;

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
            return new Thread(r, "McGrady #NetworkDispatcher #" + mCount.getAndIncrement());
        }
    };

    //阻塞队列 用于保存任务
    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(128);

    //线程池
    private static final ThreadPoolExecutor sEXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAXIMUM_POOL_SIZE,
            KEEP_ALIVE,
            TimeUnit.SECONDS,
            sPoolWorkQueue,
            sThreadFactory
    );


    public NetworkDispatcher(Cache mCache,HttpStack mStack,LinkedBlockingQueue<ImageRequest> mNetworkQueue) {
        this(mCache,mStack,mNetworkQueue,sEXECUTOR);
    }


    public NetworkDispatcher(Cache mCache,HttpStack mStack,LinkedBlockingQueue<ImageRequest> mNetworkQueue, ThreadPoolExecutor mExecutor) {
        super("MyGrady #NetworkDispatcher");
        this.mCache = mCache;
        this.mNetworkQueue = mNetworkQueue;
        this.mExecutor = mExecutor;
        this.mStack = mStack;
    }

    @Override
    public void run() {
        super.run();
        while(true){
            ImageRequest take = null;
            try {
                Log.i(TAG, "阻塞中");
                take = mNetworkQueue.take();
                Log.i(TAG, "已拿到请求"+"网络队列中任务数量:"+mNetworkQueue.size());
            } catch (InterruptedException e) {
                if(stop){
                    return;
                }
                continue;
            }

            RequestTask task = new RequestTask(take);
            if(mExecutor == null){
                throw new IllegalArgumentException("mExecutor不能为null啊,要不你就用默认的嘛");
            }
            Log.i(TAG, "把请求放入线程池中运行");
            mExecutor.execute(task);
        }
    }

    public void stopDispatcher(){
        stop = true;
        //停止线程池
        mExecutor.shutdownNow();
        //停止当前线程
        interrupt();
    }


    private class RequestTask implements Runnable{

        private ImageRequest request;

        public RequestTask(ImageRequest request) {
            this.request = request;
        }

        @Override
        public void run(){
            Bitmap bitmap = null;
            try {
                Log.i(TAG, "线程池中的线程正在执行请求");
                bitmap = mStack.performRequest(request);

                //如果图片不为空 根据策略选择地就将图片保存起来
                if(bitmap!=null){
                    mCache.toCache(request.getUrl(),bitmap);
                }

                Log.i(TAG, "线程池中的线程请求完毕 回调结果");
                request.deliveryResult(bitmap);
            } catch (IOException e) {
                Log.i(TAG, "线程池中的线程请求发生错误 回调错误");
                request.deliveryError(e);
            }

        }
    }
}
