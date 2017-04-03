# McGrady
一个简单的图片加载框架。



##update
2017.4.3

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


## how to use
```java
McGrady.from(Context)
.diskCacheStrategy(DiskCacheStrategy.CACHE+DiskCacheStrategy.DISK)
.placeHolder(R.drawable.loadImg)
.error(R.drawable.errorImg)
.load(url,imageView);
```