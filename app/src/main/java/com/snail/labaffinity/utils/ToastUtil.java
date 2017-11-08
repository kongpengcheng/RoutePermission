package com.snail.labaffinity.utils;

import android.widget.Toast;
/**
 * Created by Harry.Kong.
 * Time 2017/11/8.
 * Description:
 */
public class ToastUtil {

    public static void show(String msg) {
        Toast.makeText(AppProfile.getAppContext(), msg, Toast.LENGTH_SHORT).show();
    }
}

