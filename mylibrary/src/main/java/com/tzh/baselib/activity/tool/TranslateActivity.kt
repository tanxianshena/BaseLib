package com.tzh.baselib.activity.tool

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.gyf.immersionbar.ImmersionBar
import com.tzh.baselib.R
import com.tzh.baselib.base.XBaseBindingActivity
import com.tzh.baselib.databinding.ActivityTranslateBinding
import com.tzh.baselib.dialog.SelectTranslateDialog
import com.tzh.baselib.dto.LanguageDto
import com.tzh.baselib.network.LibNetWorkApi
import com.tzh.baselib.util.toDefault

/**
 * 翻译页面
 */
class TranslateActivity : XBaseBindingActivity<ActivityTranslateBinding>(R.layout.activity_translate) {
    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, TranslateActivity::class.java))
        }
    }

    var mFrom = LanguageDto("中文","zh")

    var mTo = LanguageDto("英语","en")

    private val mDialog by lazy {
        SelectTranslateDialog(this,object : SelectTranslateDialog.SelectTranslateListener{
            override fun sure(from: LanguageDto, to: LanguageDto) {
                mFrom = from
                mTo = to
                binding.tvTextForm.text = from.text
                binding.tvTextTo.text = to.text
            }
        })
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
    fun translate(){
        val text = binding.etText1.text.toString()
        if(text.isEmpty()){
            Toast.makeText(this,"请输入要翻译的内容",Toast.LENGTH_LONG).show()
        }else{
            LibNetWorkApi.translate(this,text,mFrom.code.toDefault("zh"),mTo.code.toDefault("en")).subscribe({
                if(it.getDataDto().size > 0){
                    val dto = it.getDataDto()[0]
                    binding.etText2.setText(dto.dst)
                }
            },{
                Toast.makeText(this,it.message,Toast.LENGTH_LONG).show()
            })
        }
    }

    fun change(){
        mDialog.show()
    }
}