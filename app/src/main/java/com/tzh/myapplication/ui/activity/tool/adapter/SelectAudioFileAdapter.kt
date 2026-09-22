package com.tzh.myapplication.ui.activity.tool.adapter

import com.tzh.baselib.adapter.XRvBindingHolder
import com.tzh.baselib.adapter.XRvBindingPureDataAdapter
import com.tzh.baselib.util.audio.GetAudioOrVideoUtil
import com.tzh.baselib.util.getSuffixName
import com.tzh.myapplication.R
import com.tzh.myapplication.databinding.AdapterSelectAudioFileBinding

class SelectAudioFileAdapter(val isSingle : Boolean,val mListener : AudioListener) : XRvBindingPureDataAdapter<GetAudioOrVideoUtil.VideoFile>(R.layout.adapter_select_audio_file) {
    //选中的列表
    val mSelectList = mutableListOf<GetAudioOrVideoUtil.VideoFile>()
    override fun onBindViewHolder(holder: XRvBindingHolder, position: Int, data: GetAudioOrVideoUtil.VideoFile) {
        val binding = holder.getBinding<AdapterSelectAudioFileBinding>()
        binding.tvName.text = data.name
        binding.tvType.text = data.name.getSuffixName()
        binding.tvMessage.text = data.getMessage()

        if(mSelectList.indexOf(data) >= 0){
            binding.ivSelect.setImageResource(R.drawable.ic_radio_selected)
        }else{
            binding.ivSelect.setImageResource(R.drawable.ic_radio_unselect)
        }

        binding.root.setOnClickListener {
            if(mSelectList.indexOf(data) >= 0){
                mSelectList.remove(data)
            }else{
                if(isSingle){
                    mSelectList.clear()
                    notifyDataSetChanged()
                }
                mSelectList.add(data)
            }
            notifyItemChanged(position)
            mListener.change(mSelectList)
        }
    }

    /**
     * 全选或者取消
     * 1全选 0取消
     */
    fun changeAll(type : Int){
        if(type == 1){
            mSelectList.clear()
            mSelectList.addAll(listData)
        }else{
            mSelectList.clear()
        }
        notifyDataSetChanged()
        mListener.change(mSelectList)
    }

    interface AudioListener{
        fun change(list : MutableList<GetAudioOrVideoUtil.VideoFile>)
    }
}