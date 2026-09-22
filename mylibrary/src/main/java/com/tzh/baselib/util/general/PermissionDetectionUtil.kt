package com.tzh.baselib.util.general

import androidx.appcompat.app.AppCompatActivity
import com.tzh.baselib.util.OnPermissionCallBackListener
import com.tzh.baselib.util.PermissionXUtil
import com.tzh.baselib.util.checkPhonePermission
import com.tzh.baselib.util.img.ChoiceImageUtil

object PermissionDetectionUtil {

    /**
     * @param isBack 没有权限的时候是否返回
     */
    fun getPermission(activity: AppCompatActivity,listener : DetectionListener,isBack : Boolean = false,isCamera : Boolean = true){
        if(isBack){
            if(ChoiceImageUtil.getPhotoPermissions(isCamera).checkPhonePermission(activity)){
                //有这个权限
                listener.ok()
            } else {
                //没有这个权限
                listener.cancel()
            }
        }else{
            getPermissionNow(activity,listener,isCamera)
        }
    }

    private fun getPermissionNow(activity: AppCompatActivity,listener : DetectionListener,isCamera : Boolean = true){
        PermissionXUtil.requestAnyPermission(activity, ChoiceImageUtil.getPhotoPermissions(isCamera),object : OnPermissionCallBackListener {
            override fun onAgree() {
                listener.ok()
            }

            override fun onDisAgree() {

            }
        })
    }

    fun getAnyPermission(activity: AppCompatActivity,list : MutableList<String>,hint : String,listener : DetectionListener){
        if(list.checkPhonePermission(activity)){
            listener.ok()
        }else{
            listener.cancel()
        }
    }

    interface DetectionListener{
        fun ok()

        fun cancel()
    }
}