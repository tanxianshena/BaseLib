package com.tzh.myapplication.ui.activity.tool.util

import com.tzh.myapplication.ui.activity.tool.dto.ConvertFormatDto

/**
 * 获取音视频转换的格式数据
 */
object ConvertDateUtil {
    /**
     * 获取音频转换格式
     */
    fun getAudioConvertList() : MutableList<ConvertFormatDto>{
        val list = mutableListOf<ConvertFormatDto>()
        list.add(ConvertFormatDto("mp3"))
        list.add(ConvertFormatDto("wav"))
        list.add(ConvertFormatDto("m4a"))
        list.add(ConvertFormatDto("amr"))
        list.add(ConvertFormatDto("aac"))
        list.add(ConvertFormatDto("flac"))
        list.add(ConvertFormatDto("ac3"))
        list.add(ConvertFormatDto("aiff"))
        list.add(ConvertFormatDto("ogg"))
        list.add(ConvertFormatDto("m4r"))
        list.add(ConvertFormatDto("mp2"))
        list.add(ConvertFormatDto("wma"))
        return list
    }

    /**
     * 获取视频转换格式
     */
    fun getVideoConvertList() : MutableList<ConvertFormatDto>{
        val list = mutableListOf<ConvertFormatDto>()
        list.add(ConvertFormatDto("mp4"))
        list.add(ConvertFormatDto("mp3"))
        list.add(ConvertFormatDto("avi"))
        list.add(ConvertFormatDto("mkv"))
        list.add(ConvertFormatDto("wmv"))
        list.add(ConvertFormatDto("mpg"))
        list.add(ConvertFormatDto("mov"))
        list.add(ConvertFormatDto("rm"))
        list.add(ConvertFormatDto("f4v"))
        list.add(ConvertFormatDto("flv"))
        list.add(ConvertFormatDto("mpeg"))
        list.add(ConvertFormatDto("webm"))
        return list
    }
}