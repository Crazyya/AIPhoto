package com.cstore.aiphoto.url

import com.google.gson.annotations.SerializedName

/**
 * Created by zhiya.zhang
 * on 2022/10/10 10:09.
 */
data class HttpResult(
        @SerializedName("result")
        val result:String,
        @SerializedName("status_code")
        val statusCode:Int,
        @SerializedName("status_message")
        val statusMessage:String)
