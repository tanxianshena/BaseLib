package com.tzh.baselib.base

import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

abstract class XBaseBindingActivity<B : ViewDataBinding>(@LayoutRes LayoutId: Int = 0) : AppCompatActivity(LayoutId) {

    protected lateinit var binding: B

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        //Activity管理
        XAppActivityManager.getInstance().addActivity(this)
        binding = DataBindingUtil.inflate(layoutInflater, layoutResID, null, false)
        setContentView(binding.root)
        init()
    }

    open fun init() {
        initView()
        initData()
    }

    protected abstract fun initView()
    protected abstract fun initData()

    override fun onDestroy() {
        XAppActivityManager.getInstance().removeActivity(this)
        super.onDestroy()
    }

}