package com.tzh.baselib

import com.tzh.baselib.network.ParamsInterceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class ParamsInterceptorTest {
    @Test fun preservesSignedTextRepeatedParametersAndExplicitHeaders() {
        val url = "https://example.test/translate".toHttpUrl().newBuilder()
            .addQueryParameter("q", " first
line ")
            .addQueryParameter("q", " second ")
            .addQueryParameter("action", "custom")
            .addQueryParameter("sign", "original-signature").build()
        val request = Request.Builder().url(url)
            .header("Accept", "text/plain").header("Accept-Language", "en").build()
        val actual = intercept(request)
        assertEquals(url, actual.url)
        assertEquals("text/plain", actual.header("Accept"))
        assertEquals("en", actual.header("Accept-Language"))
        assertNull(actual.header("Content-Type"))
    }

    @Test fun addsDefaultsOnceWithoutChangingExistingQueryEncoding() {
        val request = Request.Builder().url("https://example.test/?q=%20a%0Ab%20&q=c").build()
        val actual = intercept(intercept(request))
        assertEquals(listOf(" a
b ", "c"), actual.url.queryParameterValues("q"))
        assertEquals(listOf("0"), actual.url.queryParameterValues("action"))
        assertEquals("application/json", actual.header("Accept"))
    }

    private fun intercept(request: Request): Request {
        var captured: Request? = null
        val client = OkHttpClient.Builder().addInterceptor(ParamsInterceptor())
            .addInterceptor { chain ->
                captured = chain.request()
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK").body("".toResponseBody()).build()
            }.build()
        client.newCall(request).execute().close()
        return checkNotNull(captured)
    }
}