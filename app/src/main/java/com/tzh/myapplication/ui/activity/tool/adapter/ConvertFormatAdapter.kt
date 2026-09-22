package com.tzh.myapplication.ui.activity.tool.adapter

import com.tzh.baselib.adapter.XRvBindingHolder
import com.tzh.baselib.adapter.XRvBindingPureDataAdapter
import com.tzh.baselib.util.setTextColorRes
import com.tzh.myapplication.R
import com.tzh.myapplication.databinding.AdapterConvertFormatBinding
import com.tzh.myapplication.ui.activity.tool.dto.ConvertFormatDto

/**
 * 转换格式适配器
 */
class ConvertFormatAdapter(val mListener : ConvertListener) : XRvBindingPureDataAdapter<ConvertFormatDto>(R.layout.adapter_convert_format){
    //选择的下标
    var mIndex = -1
    override fun onBindViewHolder(holder: XRvBindingHolder, position: Int, data: ConvertFormatDto) {
        val binding = holder.getBinding<AdapterConvertFormatBinding>()
        binding.tvName.text = data.type
        if(mIndex == position){
            binding.tvName.setShapeBackgroundColorRes(R.color.color_FF666B)
            binding.tvName.setTextColorRes(R.color.color_fff)
        }else{
            binding.tvName.setShapeBackgroundColorRes(R.color.color_f7f7f7)
            binding.tvName.setTextColorRes(R.color.color_333)
        }

        binding.tvName.setOnClickListener {
            select(position)
        }
    }

    fun select(index : Int){
        val old = mIndex
        mIndex = index
        notifyItemChanged(old)
        notifyItemChanged(mIndex)
        mListener.ok(listData[index])
    }

    interface ConvertListener{
        fun ok(data: ConvertFormatDto)
    }
}