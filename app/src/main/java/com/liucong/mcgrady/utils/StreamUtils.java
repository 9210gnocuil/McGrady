package com.liucong.mcgrady.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by liucong on 2017/3/21.
 */

public class StreamUtils {

    //关闭流
    public static void close(Closeable stream){
        if(stream!=null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
