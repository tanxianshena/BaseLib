package com.tzh.myapplication.utils.screen

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import com.tzh.baselib.util.LogUtils

class ImageProcess {
    fun processScreenshot(screenshotPath: String,listener : ScreenshotObserver.ImageListener) {
        Thread{
            val bitmap = BitmapFactory.decodeFile(screenshotPath)
            if(bitmap != null){
                LogUtils.e("解析====","开始识别")
                Handler(Looper.getMainLooper()).post {
                    listener.image(bitmap)
                }
            }else{
                LogUtils.e("解析结果====","图片为空")
            }
        }.start()

    }
}