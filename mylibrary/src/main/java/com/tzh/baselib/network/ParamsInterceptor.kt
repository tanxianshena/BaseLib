package com.tzh.baselib.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** Adds defaults without changing signed query values or caller-supplied headers. */
class ParamsInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.newBuilder()
        if ("action" !in request.url.queryParameterNames) {
            url.addQueryParameter("action", "0")
        }
        val builder = request.newBuilder().url(url.build())
        if (request.header("Accept") == null) {
            builder.header("Accept", "application/json")
        }
        if (request.header("Accept-Language") == null) {
            builder.header("Accept-Language", "zh")
        }
        // Content-Type belongs to the request body (JSON, form, multipart, etc.).
        return chain.proceed(builder.build())
    }
}