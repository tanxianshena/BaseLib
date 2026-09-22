package com.tzh.myapplication.ui.activity.tool

import android.content.Context
import android.content.Intent
import android.view.View
import com.tzh.baselib.base.XAppActivityManager
import com.tzh.baselib.util.GsonUtil
import com.tzh.baselib.util.LogUtils
import com.tzh.baselib.util.audio.GetAudioOrVideoUtil
import com.tzh.baselib.util.initAdapter
import com.tzh.baselib.util.linear
import com.tzh.baselib.util.toDefault
import com.tzh.baselib.util.verDivider
import com.tzh.myapplication.R
import com.tzh.myapplication.base.AppBaseActivity
import com.tzh.myapplication.databinding.ActivitySelectAudioOrVideoBinding
import com.tzh.myapplication.ui.activity.tool.adapter.SelectAudioFileAdapter
import com.tzh.myapplication.utils.ToastUtil
import com.tzh.myapplication.utils.img.PermissionDetectionUtil

/**
 * 选择音视频页面
 */
class SelectAudioOrVideoActivity : AppBaseActivity<ActivitySelectAudioOrVideoBinding>(R.layout.activity_select_audio_or_video){

    companion object{
        /**
         * @param type 类型 audio音频 video视频
         * @param isSingle 是否是单选模式
         */
        fun start(context: Context,type : String,isSingle : Boolean = false){
            PermissionDetectionUtil.getFilePermission(XAppActivityManager.getInstance().currentActivity(),object : PermissionDetectionUtil.DetectionListener {
                override fun ok() {
                    context.startActivity(Intent(context,SelectAudioOrVideoActivity::class.java).apply {
                        putExtra("type",type)
                        putExtra("isSingle",isSingle)
                    })
                }
            })
        }
    }

    /**
     * 类型
     */
    val mType by lazy {
        intent.getStringExtra("type").toDefault("audio")
    }

    /**
     * 是否是单选模式
     */
    val isSingle by lazy {
        intent.getBooleanExtra("isSingle",false)
    }

    val mAdapter by lazy {
        SelectAudioFileAdapter(isSingle,object : SelectAudioFileAdapter.AudioListener{
            override fun change(list: MutableList<GetAudioOrVideoUtil.VideoFile>) {
                binding.tvHint.text = "已选择:${list.size}个文件"
            }
        })
    }

    override fun initView() {
        binding.activity = this
        binding.recycleView.linear().initAdapter(mAdapter).verDivider(10f)

        if(isSingle){
            //如果是单选模式
            binding.tvAll.visibility = View.GONE
            binding.tvCancel.visibility = View.GONE
        }else{
            binding.tvAll.visibility = View.VISIBLE
            binding.tvCancel.visibility = View.VISIBLE
        }

        val audioList = GetAudioOrVideoUtil.getAudioFiles(this)
        val videoList = GetAudioOrVideoUtil.getVideoFiles(this)

        if(mType == "audio"){
            mAdapter.setDatas(audioList)
        }else{
            mAdapter.setDatas(videoList)
        }
        LogUtils.e("audio====",GsonUtil.GsonString(audioList))

        LogUtils.e("video====",GsonUtil.GsonString(videoList))
    }

    override fun initData() {

    }

    /**
     * 全选或者取消
     * 1全选 0取消
     */
    fun changeAll(type : Int){
        mAdapter.changeAll(type)
    }

    fun next(){
        if(mAdapter.mSelectList.size > 0){
            AudioConvertActivity.start(this,mAdapter.mSelectList,mType)
        }else{
            ToastUtil.show("请选择文件")
        }
    }
}