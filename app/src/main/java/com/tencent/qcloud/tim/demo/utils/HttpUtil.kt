package com.tencent.qcloud.tim.demo.utils

import android.webkit.CookieManager
import com.google.gson.Gson
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object HttpUtil {
    const val LOCAL_ADDRESS = "http://150.158.151.86:8090"
    private val TYPE_JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
    private val cookieManager = CookieManager.getInstance()

    fun postRequest(
        address: String,
        body: HashMap<String, String>,
        callback: Callback
    ) {
        val requestBuilder = Request.Builder()
            .url(address)
            .post(Gson().toJson(body).toRequestBody(TYPE_JSON!!))
        val request = requestBuilder.build()
        OkHttpClient().newCall(request).enqueue(callback)
    }

    fun postRequest(
        address: String,
        body: HashMap<String, String>,
        cookie: String,
        callback: Callback
    ) {
        val requestBuilder = Request.Builder()
            .url(address)
            .post(Gson().toJson(body).toRequestBody(TYPE_JSON!!))

        val request =
            requestBuilder.addHeader("Cookie", cookie)
                .build()
        OkHttpClient().newCall(request).enqueue(callback)
    }
}