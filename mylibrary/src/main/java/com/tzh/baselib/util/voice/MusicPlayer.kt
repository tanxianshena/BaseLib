package com.tzh.baselib.util.voice

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.tzh.baselib.util.LogUtils
import com.tzh.baselib.util.sound.VoiceFileDownloadHelper
import com.tzh.baselib.util.toDefault
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class MusicPlayer(context: Context,val ok : (duration : Int) -> Unit = {},val stop : () -> Unit = {}) {

    private val helper by lazy {
        VoiceFileDownloadHelper(context)
    }

    /**
     * 是否暂停
     */
    private var isPause = false

    private var mUrl = ""

    private var mediaPlayer : MediaPlayer ?= null

    fun init(){

    }

    /**
     * 播放音乐
     * @param url 音频地址
     * @param isLoop 是否循环播放
     */
    fun play(url : String,isLoop : Boolean){
        LogUtils.e("===",url)
        LogUtils.e("===",helper.getPath(url))
        if(helper.isHaveFile(url)){
            LogUtils.e("=====","isHaveFile")
            playVoice(url,isLoop)
        }else{
            helper.onDownloadFile(url,url,object : VoiceFileDownloadHelper.OnDownloadListener() {
                override fun onStart() {
                    //"下载中(0%)"
                }

                override fun onProgress(percent: String) {
                    //"下载中($percent%)"
                    LogUtils.e("=====",percent.toString())
                }

                override fun onSuccess(file: File) {
                    LogUtils.e("=====","onSuccess")
                    playVoice(url,isLoop)
                }

                override fun onError(throwable: Throwable) {
                    LogUtils.e(throwable)
                    when(throwable){
                        is ConnectException, is NullPointerException, is SocketTimeoutException, is UnknownHostException -> {
                            //"网络错误"
                        }
                        else ->{
                            //"下载错误"
                        }

                    }
                }
            })
        }
    }

    private fun playVoice(url : String,isLoop : Boolean){
        if(mediaPlayer == null){
            mediaPlayer = MediaPlayer()
        }
        if(mUrl == helper.getPath(url) && mediaPlayer!!.isPlaying){
            mediaPlayer?.pause()
            return
        }

        if(mediaPlayer!!.isPlaying){
            mediaPlayer!!.stop()
        }


        if(mUrl == helper.getPath(url)){
            handler.post(runnable)
            mediaPlayer?.start()
        }else{
            mUrl = helper.getPath(url)
            mediaPlayer?.isLooping = isLoop
            mediaPlayer?.setDataSource(helper.getPath(url))

            mediaPlayer?.setOnPreparedListener {
                handler.post(runnable)
                mediaPlayer?.start()
            }
            mediaPlayer?.setOnCompletionListener {
                stop()
            }
            mediaPlayer?.setOnErrorListener { p0, p1, p2 ->
                stop()
                playVoice(url,isLoop)
                false
            }
            mediaPlayer?.prepareAsync()
        }
    }

    fun stop(){
        stop.invoke()
        handler.removeCallbacks(runnable)
        mUrl= ""
        isPause = false
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    val handler = Handler(Looper.getMainLooper())
    val runnable = object : Runnable {
        override fun run() {
            val currentPos = mediaPlayer?.currentPosition
//            Log.e("ExoPlayer===", "当前进度: $currentPos")
            handler.postDelayed(this, 100)
            ok.invoke(currentPos.toDefault(0))
        }
    }

    /**
     *
     */
    fun seekTo(duration: Int){
        mediaPlayer?.seekTo(duration)
    }

    /**
     * 暂停
     */
    fun pause(){
        if(mediaPlayer?.isPlaying.toDefault(false)){
            isPause = true
            mediaPlayer?.pause()
            handler.removeCallbacks(runnable)
        }
    }

    /**
     * 播放
     */
    fun start(){
        if(isPause){
            isPause = false
            mediaPlayer?.start()
            handler.post(runnable)
        }
    }

    fun isPlay() : Boolean{
        return mediaPlayer?.isPlaying.toDefault(false)
    }

    fun isPause() : Boolean{
        return isPause
    }
}