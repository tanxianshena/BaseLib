package com.tzh.myapplication.ui.activity

import android.content.Context
import android.content.Intent
import com.tzh.baselib.util.gradDivider
import com.tzh.baselib.util.grid
import com.tzh.baselib.util.initAdapter
import com.tzh.myapplication.R
import com.tzh.myapplication.base.AppBaseActivity
import com.tzh.myapplication.databinding.ActivityImageBinding
import com.tzh.baselib.util.voice.RecordView
import com.tzh.myapplication.ui.adapter.ImageAdapter
import java.io.File


class ImageActivity : AppBaseActivity<ActivityImageBinding>(R.layout.activity_image) {
    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, ImageActivity::class.java))
        }
    }

    val mAdapter by lazy {
        ImageAdapter()
    }

    override fun initView() {
        binding.recyclerView.grid(4).initAdapter(mAdapter).gradDivider(15f,4)
        mAdapter.setDatas(mutableListOf<String>().apply {
            add("https://oss.corpsoft.cn/messenger/20260822161557627336.jpg")
            add("https://oss.corpsoft.cn/messenger/20260822161601360716.jpg")
            add("https://oss.corpsoft.cn/messenger/20260822161557627336.jpg")
            add("https://oss.corpsoft.cn/messenger/20260822161601360716.jpg")
            add("https://oss.corpsoft.cn/messenger/20260822161557627336.jpg")
            add("https://oss.corpsoft.cn/messenger/20260822161601360716.jpg")
            add("https://oss.corpsoft.cn/messenger/20260822161557627336.jpg")
            add("https://oss.corpsoft.cn/messenger/20260822161601360716.jpg")
            add("https://oss.corpsoft.cn/messenger/20260822161557627336.jpg")
        })
    }

    override fun initData() {

    }

}