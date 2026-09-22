package com.tzh.myapplication.ui.activity.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import com.tzh.baselib.activity.tool.DeepSeekActivity
import com.tzh.myapplication.R
import com.tzh.myapplication.base.AppBaseActivity
import com.tzh.myapplication.databinding.ActivityMainBinding
import com.tzh.myapplication.livedata.AutoDataLiveData
import com.tzh.myapplication.ui.activity.ContactsActivity
import com.tzh.myapplication.ui.activity.HandSendMessageActivity
import com.tzh.myapplication.ui.activity.ImageActivity
import com.tzh.myapplication.ui.activity.ListActivity
import com.tzh.myapplication.ui.activity.SearchActivity
import com.tzh.myapplication.ui.activity.SendMessageActivity
import com.tzh.myapplication.ui.activity.SendSmsActivity
import com.tzh.myapplication.ui.activity.encipher.EncipherActivity
import com.tzh.myapplication.ui.activity.wallper.WallPaperActivity
import com.tzh.myapplication.ui.dialog.AddMobileDialog
import com.tzh.myapplication.ui.dialog.MyDialog
import com.tzh.myapplication.utils.ConfigUtil
import com.tzh.myapplication.utils.TimeUtil
import com.tzh.myapplication.utils.ToastUtil
import com.tzh.myapplication.utils.img.PermissionDetectionUtil
import com.tzh.myapplication.utils.window.WindowUtil
import com.tzh.baselib.activity.tool.ScanUtilActivity
import com.tzh.baselib.activity.tool.TranslateActivity
import com.tzh.baselib.livedata.observeForeverNoBack
import com.tzh.baselib.util.permission.PermissionLauncher
import com.tzh.baselib.util.picture.PictureSelectorHelper
import com.tzh.baselib.util.toDefault
import com.tzh.myapplication.ui.activity.GestureLockActivity
import com.tzh.myapplication.ui.activity.tool.SelectAudioOrVideoActivity
import com.tzh.myapplication.utils.AndroidUtil


class MainActivity : AppBaseActivity<ActivityMainBinding>(R.layout.activity_main){

    companion object{
        fun start(context: Context){
            context.startActivity(Intent(context,MainActivity::class.java))
        }
    }

    private var curSelDate: String = ""


    private val mDialog by lazy {
        MyDialog(this)
    }

    val mWindowUtil by lazy {
        WindowUtil(this)
    }

    override fun initView() {
        binding.v = this

        curSelDate = TimeUtil.getCurrentDate()

        binding.tvWza.setOnClickListener {
            if(!AndroidUtil.isIgnoringBatteryOptimizations(this)){
                AndroidUtil.requestIgnoreBatteryOptimizations(this)
            }else{

            }
        }


        AutoDataLiveData.instance.observeForeverNoBack {
            it?.apply {
                binding.root.post {
                    mWindowUtil.showAsFloatingWindow()
                }
            }
        }
    }

    override fun initData() {

    }

    fun toRecycler(){
        ListActivity.start(this)
    }

    fun openDialog(){
        mDialog.show()
    }

    fun toImage(){
        ImageActivity.start(this)
    }

    fun start(){
        AddMobileDialog(this,object : AddMobileDialog.AddMobileListener{
            override fun mobile(mobile: String) {
                ConfigUtil.setMobile(mobile)
                ToastUtil.show("修改成功")
            }
        }).show(ConfigUtil.getMobile())
    }

    fun sendMessage(){
        SendMessageActivity.start(this)
    }

    fun handSendMessage(){
        HandSendMessageActivity.start(this)
    }

    fun sendSms(){
        SendSmsActivity.start(this)
    }

    fun toWebView(){
        ContactsActivity.start(this)
    }

    /**
     * 扫码
     */
    fun scannerCode(){
        ScanUtilActivity.start(this@MainActivity,object : ScanUtilActivity.ScanListener{
            override fun sure(text: String) {
                ToastUtil.show(text)
            }

            override fun cancel() {
                ToastUtil.show("取消")
            }
        })
    }

    /**
     * 翻译
     */
    fun translate(){
        TranslateActivity.start(this)
    }

    /**
     * 搜索
     */
    fun search(){
        SearchActivity.start(this)
    }

    fun selectImg(){
        PermissionDetectionUtil.getPermission(object : PermissionDetectionUtil.DetectionListener{
            override fun ok() {
                PictureSelectorHelper.onPictureSelector(this@MainActivity,2,object : OnResultCallbackListener<LocalMedia>{
                    override fun onResult(result: ArrayList<LocalMedia>?) {
                        if(result?.size.toDefault(0) > 0){
                            val dto = result?.get(0)
                            ToastUtil.show(dto?.realPath)
                        }
                    }

                    override fun onCancel() {

                    }
                }, SelectMimeType.ofImage())
            }
        })
    }

    /**
     * 木鱼
     */
    fun muYu(){

    }

    /**
     * 添加一条短信
     */
    fun addMessage(){

    }

    /**
     * 录音
     */
    fun record(){
        ImageActivity.start(this)
    }

    /**
     * 拍照
     */
    fun openCamera(){
        PictureSelectorHelper.openCamera(this,object : OnResultCallbackListener<LocalMedia>{
            override fun onResult(result: ArrayList<LocalMedia>?) {

            }

            override fun onCancel() {

            }
        })
    }

    /**
     * 缩小为悬浮窗
     */
    fun window(){
        mWindowUtil.showAsFloatingWindow()
    }

    /**
     * 导出数据为xlsx表
     */
    fun toXls(){
        DeepSeekActivity.start(this)
    }

    /**
     * 壁纸
     */
    fun toWall(){
        WallPaperActivity.start(this)
    }

    /**
     * 加密
     */
    fun selectFile(){
        EncipherActivity.start(this)
    }

    fun toSelectFile(){
        SelectFileActivity.start(this)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun permission(){
        PermissionLauncher()
            .with(this) //this可以是androidx.fragment.app.Fragment或者androidx.fragment.app.FragmentActivity
            .granted {
                Log.e("======","已经获得权限")
            }
            .denied { rejectPermissionList: List<String> ->
                for(permission in rejectPermissionList){
                    if(permission == Manifest.permission.MANAGE_EXTERNAL_STORAGE && !Environment.isExternalStorageManager()){
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    }
                }
                Log.e("======", com.tzh.baselib.util.GsonUtil.GsonString(rejectPermissionList))
            }
            .request(getFilePermissions().toTypedArray(), arrayOf("文件选择"), arrayOf("该功能需要获取手机存储读取权限用于选择本地文件"))
    }


    /**
     * 选择文件所需权限
     */
    fun getFilePermissions(): MutableList<String> {
        return mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    /**
     * 手势解锁
     */
    fun gestureLock(){
        GestureLockActivity.start(this)
    }

    /**
     * 选择音视频
     */
    fun selectAudioOrVideo(type : String){
        SelectAudioOrVideoActivity.start(this,type,true)
    }
}