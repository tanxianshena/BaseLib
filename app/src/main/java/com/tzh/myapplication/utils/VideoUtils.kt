package com.tzh.myapplication.utils

import android.media.MediaMetadataRetriever
import kotlin.text.toLong

object VideoUtils {
    /**
     * 获取视频时长
     *
     * @param videoPath 视频地址
     * @return 视频时长 单位毫秒
     */
    fun getVideoDuration(videoPath: String?): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoPath, mutableMapOf<String, String>())
        val duration =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLong()
        retriever.release()
        if(duration != null) return duration
        return 0
    }
}