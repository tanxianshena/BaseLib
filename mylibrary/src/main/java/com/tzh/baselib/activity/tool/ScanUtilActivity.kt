package com.tzh.baselib.activity.tool

import android.content.Context
import android.content.Intent
import android.util.Log
import com.tzh.baselib.R
import com.tzh.baselib.base.XBaseBindingActivity
import com.tzh.baselib.databinding.ActivityScanUtilBinding
import com.tzh.baselib.livedata.ActivityCloseLiveData
import com.tzh.baselib.livedata.observeForeverNoBack

class ScanUtilActivity : XBaseBindingActivity<ActivityScanUtilBinding>(R.layout.activity_scan_util) {
    companion object {
        @JvmStatic
        fun start(context: Context,listener : ScanListener) {
            mListener = listener
            context.startActivity(Intent(context, ScanUtilActivity::class.java))
        }

        var mListener : ScanListener?= null
    }

    override fun initView() {
        ScanActivity.start(this)
        ActivityCloseLiveData.instance.observeForeverNoBack {

        }
    }

    override fun initData() {

    }

    interface ScanListener{
        fun sure(text : String)

        fun cancel()
    }
}