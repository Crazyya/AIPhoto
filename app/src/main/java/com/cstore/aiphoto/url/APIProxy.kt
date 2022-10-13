package com.cstore.aiphoto.url

import kotlinx.coroutines.Deferred
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Created by zhiya.zhang
 * on 2022/10/10 9:48.
 */
object APIProxy {
    const val API_BASE_TEST_URL: String = "http://192.168.3.44:50001/"
    const val API_BASE_URL1: String = "http://192.168.3.53:30001/"
    const val API_BASE_URL2: String = "http://192.168.3.52:30001/"

    interface FileAPI {
        @Multipart
        @POST("uploadShelvesImg")
        fun postImgAsync(@Part partLis: List<MultipartBody.Part>): Deferred<HttpResult>
    }
}