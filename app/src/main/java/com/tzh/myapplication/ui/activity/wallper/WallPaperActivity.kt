package com.tzh.myapplication.ui.activity.wallper

import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.angcyo.tablayout.delegate.ViewPager1Delegate
import com.tzh.myapplication.R
import com.tzh.myapplication.base.AppBaseActivity
import com.tzh.myapplication.databinding.ActivityWallPaperBinding
import com.tzh.baselib.util.DpToUtil
import com.tzh.baselib.util.toDefault

class WallPaperActivity : AppBaseActivity<ActivityWallPaperBinding>(R.layout.activity_wall_paper){
    companion object {
        @JvmStatic
        fun start(context : Context) {
            context.startActivity(Intent(context, WallPaperActivity::class.java))
        }
    }

    override fun initView() {

    }

    override fun initData() {
        binding.root.postDelayed({

        },200)
    }

    fun setList(list : MutableList<WallCate>){
        val fragmentList = mutableListOf<Fragment>()
        for(dto in list){
            val tv = TextView(this)
            tv.gravity = Gravity.CENTER
            tv.setPadding(DpToUtil.dip2px(tv.context, 20f), 0, DpToUtil.dip2px(tv.context, 20f), 0)
            tv.text = dto.name
            binding.tabLayout.addView(tv)
            fragmentList.add(SortFragment.getInstance(dto.cateId.toDefault("")))
        }

        ViewPager1Delegate.install(binding.viewPager, binding.tabLayout, true)
//        binding.viewPager.offscreenPageLimit = fragmentList.size
        binding.viewPager.adapter = ViewPageAdapter(supportFragmentManager,fragmentList)
    }
}