package com.tzh.baselib.activity.tool

import android.app.Activity
import android.os.Bundle
import com.tzh.baselib.R
import com.tzh.baselib.base.XBaseBindingActivity
import com.tzh.baselib.databinding.ActivityScanBinding
import com.tzh.baselib.livedata.ActivityCloseLiveData
import com.tzh.baselib.util.toDefault

class ScanActivity : XBaseBindingActivity<ActivityScanBinding>(R.layout.activity_scan) {
    companion object {
        @JvmStatic
        fun start(context: Activity) {

        }
    }

    private val mTitle by lazy {
        intent.getStringExtra("title")
    }

    override fun initView() {
        binding.titleBar.setTitleTxt(mTitle.toDefault("扫码"))

    }

    override fun initData() {

    }

    override fun onResume() {
        super.onResume()
    }
}