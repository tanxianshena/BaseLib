package com.tzh.myapplication.ui.activity

import android.content.Context
import android.content.Intent
import com.tzh.baselib.util.LogUtils
import com.tzh.myapplication.R
import com.tzh.myapplication.base.AppBaseActivity
import com.tzh.myapplication.databinding.ActivityGestureLockBinding
import com.tzh.myapplication.utils.ToastUtil

class GestureLockActivity : AppBaseActivity<ActivityGestureLockBinding>(R.layout.activity_gesture_lock) {

    companion object{
        fun start(context: Context){
            context.startActivity(Intent(context,GestureLockActivity::class.java))
        }
    }

    override fun initView() {
        binding.gestureLockView.setOnGestureLockListener {
            LogUtils.e("=====",it)
            if (isGestureCorrect(it)) {
                ToastUtil.show("手势解锁成功")
                finish()
            } else {
                ToastUtil.show("手势解锁失败")
            }
        }
    }

    override fun initData() {

    }

    private fun isGestureCorrect(gesture: String): Boolean {
        // 这里应该与预设的手势进行比较
        val correctGesture = "360,360;720,360;1080,360;1080,720;1080,1080;"
        return gesture == correctGesture
    }
}