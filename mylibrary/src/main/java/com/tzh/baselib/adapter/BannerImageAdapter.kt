package com.tzh.baselib.adapter

import android.view.View
import com.bumptech.glide.Glide
import com.tzh.baselib.R
import com.tzh.baselib.databinding.BannerAdapterBinding

class BannerImageAdapter(data: List<String>, val listener: View.OnClickListener) :
    BaseBannerAdapter<String>(R.layout.banner_adapter, data) {

    override fun onBindView(holder: XRvBindingHolder, data: String, position: Int, size: Int) {
        holder.getBinding<BannerAdapterBinding>().apply {
            image.setScaleLevels(1f, 2.5f, 5f)
            image.setAllowParentInterceptOnEdge(true)
            // PhotoView dispatches this only after a confirmed single tap; double taps zoom.
            image.setOnClickListener { listener.onClick(it) }
            Glide.with(image).load(data).dontAnimate().into(image)
        }
    }

    override fun onViewRecycled(holder: XRvBindingHolder) {
        holder.getBinding<BannerAdapterBinding>().image.apply {
            setOnClickListener(null)
            if (drawable != null) setScale(minimumScale, false)
            Glide.with(this).clear(this)
        }
        super.onViewRecycled(holder)
    }
}
