package com.tzh.myapplication.ui.activity

import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.tzh.baselib.view.SearchView
import com.tzh.myapplication.R
import com.tzh.myapplication.base.AppBaseActivity
import com.tzh.myapplication.databinding.ActivitySearchBinding
import com.tzh.myapplication.utils.ToastUtil
import com.tzh.myapplication.utils.screen.ScreenshotObserver


class SearchActivity : AppBaseActivity<ActivitySearchBinding>(R.layout.activity_search) {
    companion object {
        @JvmStatic
        fun start(context: AppCompatActivity) {
            context.startActivity(Intent(context, SearchActivity::class.java))
        }
    }

    override fun initView() {
        binding.activity = this

        binding.searchView.setSvSearchListener(object : SearchView.SvSearchListener{
            override fun search(text: String): Boolean {
                ToastUtil.show(text)
                return true
            }

            override fun clear() {

            }
        })

        val handler = Handler()
        val observer = ScreenshotObserver(handler, this,object : ScreenshotObserver.ImageListener{
            override fun image(bitmap: Bitmap) {
                binding.ivImage.setImageBitmap(bitmap)

            }
        })
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
    }

    override fun initData() {

    }
}