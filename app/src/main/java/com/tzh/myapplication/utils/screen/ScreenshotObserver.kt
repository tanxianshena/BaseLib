package com.tzh.myapplication.utils.screen

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import com.tzh.baselib.util.LogUtils
import com.tzh.baselib.util.toDefault


class ScreenshotObserver(handler: Handler, context: Context,val listener : ImageListener) : ContentObserver(handler) {
    private val context: Context

    init {
        this.context = context
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        if(uri != null){
            val screenshotPath = getScreenshotPath(uri)
            LogUtils.e("截屏====",screenshotPath.toDefault(""))
            if (screenshotPath?.contains("Screenshot",true).toDefault(false)) {
                // 检测到截屏
                LogUtils.e("截屏====","检测到截屏")
                screenshotPath?.let { ImageProcess().processScreenshot(it,listener) }
            }
        }
    }

    private fun getScreenshotPath(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                return cursor.getString(columnIndex)
            }
            cursor.close()
        }
        return null
    }

    interface ImageListener{
        fun image(bitmap : Bitmap)
    }

}