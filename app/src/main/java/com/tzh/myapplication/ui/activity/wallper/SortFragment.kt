package com.tzh.myapplication.ui.activity.wallper

import com.tzh.myapplication.R
import com.tzh.myapplication.databinding.FragmentSortBinding
import com.tzh.baselib.base.XBaseBindingFragment
import com.tzh.baselib.util.gradDivider
import com.tzh.baselib.util.grid
import com.tzh.baselib.util.initAdapter

/**
 * 分类 fragment
 */
class SortFragment : XBaseBindingFragment<FragmentSortBinding>(R.layout.fragment_sort){

    companion object{
        fun getInstance(id : String) : SortFragment{
           val fragment = SortFragment()
            fragment.setData(id)
           return fragment
        }
    }

    val mAdapter by lazy {
        WallpaperListAdapter()
    }

    /**
     * 分类ID
     */
    var mId : String ?= null

    fun setData(id : String){
        mId = id
    }

    override fun onInitView() {
        binding.fragment = this
        binding.recycleView.grid(3).initAdapter(mAdapter).gradDivider(6f,3)
        binding.smartLayout.setOnRefreshLoadMoreListener {
            getData()
        }

        getData()
    }

    override fun onLoadData() {

    }

    private fun getData(){

    }
}