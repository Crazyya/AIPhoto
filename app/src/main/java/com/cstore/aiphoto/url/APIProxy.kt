package com.cstore.aiphoto.url

import com.cstore.aiphoto.MyTimeUtil
import kotlinx.coroutines.Deferred
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

/**
 * Created by zhiya.zhang
 * on 2022/10/10 9:48.
 */
object APIProxy {
    const val API_BASE_TEST_URL: String = "http://192.168.3.44:50001/"
    const val API_BASE_URL1: String = "http://192.168.3.53:30001/"
    const val API_BASE_URL2: String = "http://192.168.3.52:30001/"
    //华东总部
    const val API_BASE_URL: String = "http://watchstore.rt-store.com:8086/app/"

    const val ERROR_FILE_HEADER = "fileName"
    interface FileAPI {
        @Multipart
        @POST("uploadShelvesImg")
        fun postImgAsync(@Part partLis: List<MultipartBody.Part>): Deferred<HttpResult>
    }

    interface ReportAPI {

        @POST("asset/uploadError.do")
        fun sendReportErrorAsync(
            @Body
            content: RequestBody,
            @Header(ERROR_FILE_HEADER)
            fileName: String = "Error/AIImage${MyTimeUtil.nowTimeString3}.txt"): Deferred<String>
    }
}