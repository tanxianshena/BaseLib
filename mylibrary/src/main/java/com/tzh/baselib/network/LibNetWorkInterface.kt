package com.tzh.baselib.network

import android.util.ArrayMap
import com.tzh.baselib.dto.DeepSeekRequestDto
import com.tzh.baselib.dto.DeepSeekResponseDto
import com.tzh.baselib.dto.TranslateDto
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.QueryMap

interface LibNetWorkInterface {

    /**
     * 翻译
     */
    @POST("https://fanyi-api.baidu.com/api/trans/vip/translate")
    fun translate(@QueryMap arrayMap: ArrayMap<String, Any>): Observable<LibBaseResDto<MutableList<TranslateDto>>>

    /**
     * AI
     */
    @POST("https://api.deepseek.com/chat/completions")
    fun sendRequest(@Header("Content-Type") type : String,@Header("Authorization") apiKey : String,@Body request: DeepSeekRequestDto): Observable<LibBaseResDto<DeepSeekResponseDto>>
}