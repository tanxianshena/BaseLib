package com.tzh.myapplication.ui.dialog

import android.content.Context
import com.tzh.myapplication.R
import com.tzh.baselib.base.BaseBindingDialog
import com.tzh.myapplication.databinding.DialogMyBinding
import com.tzh.baselib.util.setOnClickNoDouble

class MyDialog(context : Context) : BaseBindingDialog<DialogMyBinding>(context,R.layout.dialog_my){
    init {
        initBottomDialog()
    }
    override fun initView() {
        binding.tvOk.setOnClickNoDouble {
            dismiss()
        }
        binding.tvClear.setOnClickNoDouble {
            dismiss()
        }
    }

    override fun initData() {

    }
}