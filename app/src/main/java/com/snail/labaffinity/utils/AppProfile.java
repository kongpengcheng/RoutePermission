package com.snail.labaffinity.utils;

import android.app.Application;

import com.snail.labaffinity.app.LabApplication;
/**
 * Created by Harry.Kong.
 * Time 2017/11/8.
 * Description:
 */
public class AppProfile {

    public static Application getAppContext() {
        return LabApplication.getContext();
    }
}
