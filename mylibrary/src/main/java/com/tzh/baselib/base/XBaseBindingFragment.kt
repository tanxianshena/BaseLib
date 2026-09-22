package com.tzh.baselib.base

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment

abstract class XBaseBindingFragment<B : ViewDataBinding>(@LayoutRes contentLayoutId: Int = 0) : Fragment(contentLayoutId) {

    private var viewBinding: B? = null
    var binding: B
        get() = checkNotNull(viewBinding) { "binding is only available between onViewCreated and onDestroyView" }
        set(value) { viewBinding = value }

    /**
     * 数据加载方式是否被调用
     */
    protected var isLoadData = false


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = DataBindingUtil.bind(view)!!
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        onInitView()
    }

    override fun onDestroyView() {
        viewBinding?.unbind()
        viewBinding = null
        isLoadData = false
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        if (!isLoadData) {
            // 将数据加载逻辑放到onResume()方法中
            isLoadData = true
            onLoadData()
        }
    }

    /**
     * 初始化视图
     */
    protected abstract fun onInitView()

    /**
     * 初始化数据
     */
    protected abstract fun onLoadData()

}