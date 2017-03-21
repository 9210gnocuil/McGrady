# McGrady
一个简单的图片加载框架。



##update
###2017.3.21
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
.diskCacheStrategy(DiskCacheStrategy.DEFAULT)
.placeHolder(R.drawable.loadImg)
.error(R.drawable.errorImg)
.load(url,imageView);
```