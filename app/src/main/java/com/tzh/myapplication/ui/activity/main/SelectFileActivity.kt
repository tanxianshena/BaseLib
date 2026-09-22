package com.tzh.myapplication.ui.activity.main

import android.content.Context
import android.content.Intent
import android.util.Log
import com.tzh.baselib.view.file.FileItem
import com.tzh.baselib.view.file.FilePickerFragment
import com.tzh.myapplication.R
import com.tzh.myapplication.base.AppBaseActivity
import com.tzh.myapplication.databinding.ActivitySelectFileBinding


/**
 * 选择文件 页面
 */
class SelectFileActivity : AppBaseActivity<ActivitySelectFileBinding>(R.layout.activity_select_file), FilePickerFragment.FileSelectListener {

    companion object{
        fun start(context: Context){
            context.startActivity(Intent(context,SelectFileActivity::class.java))
        }
    }

    override fun initView() {
        showFilePicker()

    }

    override fun initData() {

    }

    private fun showFilePicker() {
        val fragment = FilePickerFragment().apply {
            // 可以在这里传递参数，如允许的文件类型
        }
        supportFragmentManager.beginTransaction()
            .add(R.id.frame_layout,fragment) // 替换your_frame_layout_id为你的FrameLayout的ID
            .commit()
    }

    override fun onFilesSelected(files: List<FileItem>) {
        // 处理选择的文件
        files.forEach { file ->
            Log.e("SelectedFile===", "Name: ${file.name}, Path: ${file.path}")
        }
    }
}
