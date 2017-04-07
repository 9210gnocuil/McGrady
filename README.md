# McGrady
一个简单的图片加载框架。



###update


2017.4.7
代码重构。具体内容不想写。

2017.4.3-2
使用线程池优化网络图片下载速度。
由于开启下载任务需要拿到任务完成返回的数据，因此借鉴Handler源码，在子线程中开启轮询，主线程中将任务相关的数据封装成一个Task类，并且将这个类作为消息添加到消息队列中。然后在子线程中轮询的的looper拿到消息并将里面的任务添加到ThreadPoolExecutor中执行，然后等待结果出来了就将结果保存到Task中并将Task作为参数发送给Handler，Handler在主线程中更新图片。


2017.4.3-1
更新缓存策略判定 简化代码

>   每次判断只需要&操作 简单

```java
public static class DiskCacheStrategy{
    public static final int NONE = 1<<1;
    public static final int CACHE = 1<<2;
    public static final int DISK = 1<<3;
}
```

2017.3.22
1.使用android中自带的lru算法缓存，替代之前用LinkedHashMap实现的lru，简化代码

```java
public static LruCache<String,Bitmap> mMemoryCache = new LruCache<String,Bitmap>(maxCacheCapacity){
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };
```

2017.3.21
1. 添加缓存策略
```java
public static class DiskCacheStrategy{
    public static final int NONE = 1;
    public static final int CACHE_ONLY = 2;
    public static final int DISK_ONLY = 3;
    public static final int DEFAULT = 0; //默认缓存内存和磁盘
}
diskCacheStrategy(int cacheStrategy)
```
2. 添加设置默认图片。
```java
placeHolder(int imgResId)//设置加载时显示的图片
error(int errImgResId)//设置加载失败时显示的图片
```


### how to use
```java
McGrady.from(Context)
.diskCacheStrategy(DiskCacheStrategy.CACHE+DiskCacheStrategy.DISK)
.placeHolder(R.drawable.loadImg)
.error(R.drawable.errorImg)
.load(url,imageView);
```