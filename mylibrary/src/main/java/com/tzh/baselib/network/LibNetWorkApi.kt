package com.tzh.baselib.network

import android.util.ArrayMap
import androidx.lifecycle.LifecycleOwner
import com.tzh.baselib.dto.DeepSeekRequestDto
import com.tzh.baselib.dto.DeepSeekResponseDto
import com.tzh.baselib.dto.MessageDto
import com.tzh.baselib.dto.TranslateDto
import com.tzh.baselib.util.TranslateUtil
import com.uber.autodispose.ObservableSubscribeProxy
import io.reactivex.Observable

object LibNetWorkApi {
    init {
        HttpHelper.onBindingInterface(LibNetWorkInterface::class.java)
    }


    /**
     * 翻译
     */
    fun translate(owner: LifecycleOwner,text : String,from : String,to : String): ObservableSubscribeProxy<LibBaseResDto<MutableList<TranslateDto>>> {
        return xHttpRequest<LibNetWorkInterface>().translate(
            ArrayMap<String, Any>().apply {
                //翻译内容
                put("q",text)
                //翻译源语言
                put("from",from)
                //翻译目标语言
                put("to",to)
                //APPID
                put("appid", TranslateUtil.appId)
                val salt = TranslateUtil().getText()
                //随机数
                put("salt",salt)
                //签名
                put("sign", TranslateUtil().translateMD5(text,salt))
            }
        ).xWithDefault(owner)
    }

    /**
     * Runtime credentials supplied by the host. Never embed a shared service key in the APK.
     * Prefer a server proxy in production; this hook supports host-managed user credentials.
     */
    @Volatile
    var deepSeekApiKeyProvider: (() -> String?)? = null
    fun sendRequest(owner: LifecycleOwner,inputText : String) : ObservableSubscribeProxy<LibBaseResDto<DeepSeekResponseDto>> {
        val request = DeepSeekRequestDto(mutableListOf<MessageDto>().apply {
            add(MessageDto(inputText,"user"))
        })
        return Observable.defer {
            val apiKey = deepSeekApiKeyProvider?.invoke()?.trim()
            if (apiKey.isNullOrEmpty()) {
                Observable.error<LibBaseResDto<DeepSeekResponseDto>>(
                    IllegalStateException("DeepSeek credentials are not configured")
                )
            } else {
                xHttpRequest<LibNetWorkInterface>().sendRequest("application/json", "Bearer $apiKey", request)
            }
        }.xWithDefault(owner)
    }
}