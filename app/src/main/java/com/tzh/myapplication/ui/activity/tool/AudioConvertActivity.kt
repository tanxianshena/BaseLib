package com.tzh.myapplication.ui.activity.tool

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.SeekBar
import com.tzh.baselib.util.GsonUtil
import com.tzh.baselib.util.audio.GetAudioOrVideoUtil
import com.tzh.baselib.util.toDefault
import com.tzh.baselib.util.voice.MusicPlayer
import com.tzh.myapplication.R
import com.tzh.myapplication.base.AppBaseActivity
import com.tzh.myapplication.databinding.ActivityAudioConvertBinding
import com.tzh.myapplication.utils.VideoUtils

/**
 * 音视频转换页面
 */
class AudioConvertActivity : AppBaseActivity<ActivityAudioConvertBinding>(R.layout.activity_audio_convert){

    companion object{
        /**
         * @param file 需要转换的数据
         * @param type 类型 audiO音频 video视频
         */
        fun start(context: Context,file : MutableList<GetAudioOrVideoUtil.VideoFile>,type : String){
            context.startActivity(Intent(context,AudioConvertActivity::class.java).apply {
                putExtra("file",GsonUtil.GsonString(file))
                putExtra("type",type)
            })
        }
    }

    val file by lazy {
        GsonUtil.GsonToList(intent.getStringExtra("file").toDefault("[]"),GetAudioOrVideoUtil.VideoFile::class.java)
    }

    val mUrl = "https://appimg.yunxiaoguo.cn/video2sound_all/20250725/ad8d240f03d947128daa35c6d1bf7eb7_i0fe92cfa695b11f0b8ae5254004ec149_vocal.aac"

    val duration by lazy {
        VideoUtils.getVideoDuration(mUrl)
    }

    val audioPlayer by lazy {
        MusicPlayer(this, ok = {
            binding.seekBar.setProgress((it * 100f / duration).toInt())
            binding.tvStart.text = formatTime(it.toFloat())
        }, stop = {
            binding.seekBar.setProgress(0)
            binding.tvStart.text = "00:00"
            binding.btnPlay.setImageResource(R.mipmap.icon_play)
        })
    }

    override fun initView() {
        binding.activity = this
        binding.tvEnd.text = formatTime(duration.toFloat())
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 进度变化时调用
                // fromUser 表示是否由用户拖动触发
                if(fromUser){
                    audioPlayer.seekTo(duration.toInt() / 100 * progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // 用户开始拖动时调用
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // 用户停止拖动时调用
            }
        })

        // 播放/暂停按钮
        binding.btnPlay.setOnClickListener {
            play()
        }
    }

    override fun initData() {

    }

    fun play(){
        if(audioPlayer.isPlay()){
            binding.btnPlay.setImageResource(R.mipmap.icon_play)
            audioPlayer.pause()
        }else{
            binding.btnPlay.setImageResource(R.mipmap.icon_stop)
            audioPlayer.play(mUrl,false)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(seconds: Float): String {
        val totalSeconds = seconds.toInt()
        val minutes = totalSeconds / 60 /1000
        val remainingSeconds = totalSeconds / 1000 % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.stop()
    }
}