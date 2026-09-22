package com.tzh.baselib.util

import android.util.Log
import com.tzh.baselib.BuildConfig

object LogUtils {
    private const val TAG = "LogUtils"

    /** Opt in for local debugging. Release builds never emit these logs. */
    @Volatile
    var enabled: Boolean = false

    @JvmStatic
    fun e(tag : String?,text : String){
        if (BuildConfig.DEBUG && enabled) Log.e(tag.toDefault(TAG),text)
    }

    @JvmStatic
    fun d(tag : String?,text : String){
        if (BuildConfig.DEBUG && enabled) Log.d(tag.toDefault(TAG),text)
    }

    @JvmStatic
    fun i(tag : String?,text : String){
        if (BuildConfig.DEBUG && enabled) Log.i(tag.toDefault(TAG),text)
    }

    @JvmStatic
    fun w(tag : String?,text : String){
        if (BuildConfig.DEBUG && enabled) Log.w(tag.toDefault(TAG),text)
    }

    @JvmStatic
    fun v(tag : String?,text : String){
        if (BuildConfig.DEBUG && enabled) Log.v(tag.toDefault(TAG),text)
    }

    fun e(t: Throwable?) {
        if (BuildConfig.DEBUG && enabled) Log.e(TAG, t.toString())
    }
}