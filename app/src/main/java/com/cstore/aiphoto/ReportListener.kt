package com.cstore.aiphoto

import android.os.Environment
import com.cstore.aiphoto.MyJavaFun.clearErrorTxtMessage
import com.cstore.aiphoto.MyJavaFun.getErrorTxtMessage
import com.cstore.aiphoto.url.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Created by zhiya.zhang
 * on 2017/7/20 8:47.
 * 上报日志
 */
object ReportListener {
    /**
     * 上传错误信息
     */
    suspend fun reportError(isException: Boolean = false) = withContext(Dispatchers.IO) {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            try {
                val dataMsg = getErrorTxtMessage()
                if (dataMsg.isNotEmpty()) {
                    val action = if (isException) "应用崩溃" else "记录错误"
                    val requestBody = "AI拍摄报错，\r\n数据：$dataMsg"
                    Api.report.sendReportErrorAsync(requestBody.toRequestBody("text/plain".toMediaTypeOrNull()))
                        .runCatching { await() }
                        .onSuccess {
                            clearErrorTxtMessage()
                        }
                        .onFailure { }
                }
            }catch (e:Exception){
                clearErrorTxtMessage()
            }
        }
        return@withContext
    }
}