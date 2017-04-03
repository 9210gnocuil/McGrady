package com.liucong.mcgrady;

import android.graphics.Bitmap;
import android.os.Message;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

/**
 * Created by liucong on 2017/4/3.
 * 任务轮询器 在子线程中轮询
 * 如果有任务就执行
 * 没有就阻塞
 */

public class TaskLooper {

    static boolean isRunning = true;

    static final ThreadLocal<TaskLooper> sThreadLocal = new ThreadLocal<TaskLooper>();

    final LinkedBlockingQueue<Task> mQueue;

    private TaskLooper(){
        mQueue = new LinkedBlockingQueue<>();
    }

    public static void prepare(){
        if(sThreadLocal.get()!=null){
            throw new RuntimeException("每个线程只能存在一个TaskLooper");
        }
        sThreadLocal.set(new TaskLooper());
    }

    public static TaskLooper myLooper(){
        return sThreadLocal.get();
    }

    public static void loop(){
        final TaskLooper looper = myLooper();
        if(looper == null){
            throw new RuntimeException("你需要一个TaskLooper 请在使用前调用TaskLooper的prepare()");
        }
        final LinkedBlockingQueue<Task> queue = looper.mQueue;

        while(isRunning) {
            try {
                Task task = queue.take();
                Future<Bitmap> future = task.getExecutor().submit(task.getCallable());
                Bitmap bitmap = future.get();

                task.setBitmap(bitmap);

                //拿到任务后发送消息给handler 在handler中设置图片
                Message msg = task.getHandler().obtainMessage();
                msg.obj = task;
                task.getHandler().sendMessage(msg);


            }catch (RejectedExecutionException e){
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
