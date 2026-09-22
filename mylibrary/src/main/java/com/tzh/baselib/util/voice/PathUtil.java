package com.tzh.baselib.util.voice;

import android.content.Context;

import com.tzh.baselib.util.AppPathManager;

import java.io.File;


public class PathUtil {

    /**
     * 获取老师端录制问答语音路径
     * @param context
     * @return
     */
    public static File getMasterQuestionVoice(Context context) {
        File file = context.getExternalFilesDir("voice/");
        assert file != null;
        AppPathManager.ifFolderExit(file.getAbsolutePath());
        return file;
    }

    /**
     * 获取存储xml文件夹
     * @param context 上下文
     * @return
     */
    public static File getXml(Context context) {
        File file = context.getExternalFilesDir("xml/");
        assert file != null;
        AppPathManager.ifFolderExit(file.getAbsolutePath());
        return file;
    }

    /**
     * 获取存储xml文件夹
     * @param context 上下文
     * @return
     */
    public static File getTessData(Context context) {
        File file = context.getExternalFilesDir("tessdata/");
        assert file != null;
        AppPathManager.ifFolderExit(file.getAbsolutePath());
        return file;
    }

    /**
     * 获取存储音频文件夹
     * @param context 上下文
     * @return
     */
    public static File getAudio(Context context) {
        File file = context.getExternalFilesDir("audio/");
        assert file != null;
        AppPathManager.ifFolderExit(file.getAbsolutePath());
        return file;
    }

    /**
     * 获取存储视频文件夹
     * @param context 上下文
     * @return
     */
    public static File getVideo(Context context) {
        File file = context.getExternalFilesDir("video/");
        assert file != null;
        AppPathManager.ifFolderExit(file.getAbsolutePath());
        return file;
    }
}
