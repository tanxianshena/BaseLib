package com.tzh.myapplication.ui.adapter

import com.tzh.baselib.activity.PhotoViewActivity
import com.tzh.baselib.adapter.XRvBindingHolder
import com.tzh.baselib.adapter.XRvBindingPureDataAdapter
import com.tzh.baselib.base.XAppActivityManager
import com.tzh.baselib.util.LoadImageUtil
import com.tzh.myapplication.R
import com.tzh.myapplication.databinding.AdapterImageBinding

class ImageAdapter : XRvBindingPureDataAdapter<String>(R.layout.adapter_image) {
    override fun onBindViewHolder(holder: XRvBindingHolder, position: Int, data: String) {
        holder.getBinding<AdapterImageBinding>().apply {
            LoadImageUtil.loadImageUrl(this.ivImage,data,10f)

            this.ivImage.setOnClickListener {
                PhotoViewActivity.start(XAppActivityManager.getInstance().currentActivity()!!,ivImage, listData as ArrayList<String>,position)
            }
        }
    }
}