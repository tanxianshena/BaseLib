package com.tzh.baselib.adapter

import android.view.View
import com.tzh.baselib.util.LoadImageUtil
import com.tzh.baselib.util.setOnClickNoDouble
import com.tzh.baselib.R
import com.tzh.baselib.databinding.BannerAdapterBinding

class BannerImageAdapter(data: List<String>,val listener : View.OnClickListener) : BaseBannerAdapter<String>(R.layout.banner_adapter,data) {

    override fun onBindView(holder: XRvBindingHolder, data: String, position: Int, size: Int) {
        holder.getBinding<BannerAdapterBinding>().apply {
            LoadImageUtil.loadImageUrl(image,data)
            image.setOnClickNoDouble {
                listener.onClick(it)
            }
        }
    }
}