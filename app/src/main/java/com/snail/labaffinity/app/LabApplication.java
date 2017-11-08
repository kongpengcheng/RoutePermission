package com.snail.labaffinity.app;

import android.app.Application;

import cn.campusapp.router.Router;

/**
 * Created by Harry.Kong.
 * Time 2017/11/8.
 * Description:
 */
public class LabApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
        Router.initBrowserRouter(this);
        Router.initActivityRouter(getApplicationContext());
    }

    private static Application sApplication;

    public static Application getContext() {
        return sApplication;
    }
}
