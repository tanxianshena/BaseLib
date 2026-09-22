package com.tzh.baselib.activity.tool

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.gyf.immersionbar.ImmersionBar
import com.tzh.baselib.R
import com.tzh.baselib.base.XBaseBindingActivity
import com.tzh.baselib.databinding.ActivityDeepSeekBinding
import com.tzh.baselib.network.LibNetWorkApi
import com.tzh.baselib.util.toDefault

/**
 * DeepSeek页面
 */
class DeepSeekActivity : XBaseBindingActivity<ActivityDeepSeekBinding>(R.layout.activity_deep_seek) {
    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, DeepSeekActivity::class.java))
        }
    }

    override fun initView() {
        ImmersionBar.with(this).transparentStatusBar().statusBarDarkFont(true).init()
        binding.activity = this

    }

    override fun initData() {

    }

    /**
     * 翻译
     */
    @SuppressLint("AutoDispose")
    fun translate(){
        val text = binding.etText1.text.toString()
        if(text.isEmpty()){
            Toast.makeText(this,"请输入要问的内容",Toast.LENGTH_LONG).show()
        }else{

            LibNetWorkApi.sendRequest(this,text).subscribe({

                if(it.choices?.size.toDefault(0) > 0){
                    binding.etText2.setText(it.choices?.get(0)?.message?.content)
                }

            },{
                Toast.makeText(this, "请求失败，请检查凭据配置和网络连接", Toast.LENGTH_LONG).show()
            })
        }
    }
}