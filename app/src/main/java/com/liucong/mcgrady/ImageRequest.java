package com.liucong.mcgrady;

import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

/**
 * Created by liucong on 2017/4/7.
 */

public class ImageRequest {

    private static final String TAG = "ImageRequest";

    private String mUrl;
    private Handler mHandler;
    private ImageView mImageView;

    private int mErrorImage;

    public ImageRequest(String mUrl, Handler mHandler, ImageView mImageView, int mErrorImage) {
        this.mUrl = mUrl;
        this.mHandler = mHandler;
        this.mImageView = mImageView;
        this.mErrorImage = mErrorImage;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String mUrl){
        this.mUrl = mUrl;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    public ImageView getImageView() {
        return mImageView;
    }

    public void setImageView(ImageView mImageView) {
        this.mImageView = mImageView;
    }

    /**
     * 将结果转发出去
     */
    public void deliveryResult(final Bitmap bitmap){

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(bitmap == null && mErrorImage != 0){
                    mImageView.setImageResource(mErrorImage);
                }
                //检查标记 以免图片错乱
                if (mUrl.equals(mImageView.getTag())){
                    Log.i(TAG, "加载成功");
                    mImageView.setImageBitmap(bitmap);
                }
            }
        });
    }

    public void deliveryError(Throwable error){

    }

    @Override
    public boolean equals(Object o) {

        ImageRequest request = (ImageRequest) o;

        //如果url是一样的就判断imageView
        return this.mUrl.equals(request.mUrl) && this.mImageView == request.mImageView;

    }
}
