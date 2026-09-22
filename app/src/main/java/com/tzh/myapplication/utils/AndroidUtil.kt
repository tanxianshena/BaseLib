package com.tzh.myapplication.utils

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.tzh.myapplication.base.MyApplication


object AndroidUtil {

    /**
     * 获取当前版本名
     */
    fun getVersionName(context: Context): String? {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }

    /**
     * 获取当前版本号
     */
    fun getVersionCode(context: Context): Int {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 复制文字
     */
    fun copy(text : String){
        val cm = MyApplication.mContext.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
        cm.text = text
        ToastUtil.showLong("复制成功")
    }

    //检测应用是否安装
    fun checkMapAppsIsExist(context: Context, packageName: String): Boolean {
        var packageInfo: PackageInfo?
        try {
            packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        } catch (e: java.lang.Exception) {
            packageInfo = null
            e.printStackTrace()
        }
        return packageInfo != null
    }

    /**
     * 检测程序是否安装
     *
     * @param packageName
     * @return
     */
    fun isInstalled(context: Context, packageName: String): Boolean {
        val manager: PackageManager = context.packageManager
        //获取所有已安装程序的包信息
        val installedPackages = manager.getInstalledPackages(0)
        for (info in installedPackages) {
            Log.e("packageName===",info.packageName)
            if (info.packageName == packageName) return true
        }
        return false
    }


    /**
     * 判断我们的应用是否在白名单中
     * @param context
     * @return
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        var isIgnoring = false
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        return isIgnoring
        //如果小于Build.VERSION_CODES.M版本，直接返回成功,不去请求了!!!
    }

    /**
     * 申请加入白名单
     * @param context
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = ("package:" + context.packageName).toUri()
            context.startActivity(intent)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}