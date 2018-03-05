package com.example.administrator.androidstudy.utils;

import android.content.res.Resources;

/**
 * Created by Administrator on 2018/3/5.
 */

public class Utils {
    public static int dip2pix(int dip) {
        float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int)(scale * dip + 0.5);
    }
}
