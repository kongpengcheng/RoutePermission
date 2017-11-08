package com.annotation;

/**
 * Created by Harry.Kong.
 * Time 2017/11/8.
 * Description:
 */
public interface OnGrantedListener<T> {

    void onGranted(T target,String[] permissions);

    void onDenied(T target,String[] permissions);

    void onNeverAsk(T target,String[] permissions);

    void onShowRationale(T target,String[] permissions);
}
