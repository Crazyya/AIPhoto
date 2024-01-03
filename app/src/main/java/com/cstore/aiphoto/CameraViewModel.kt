package com.cstore.aiphoto

import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cstore.aiphoto.url.APIProxy
import com.cstore.aiphoto.url.Api
import com.cstore.aiphoto.url.HttpResult
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*

/**
 * Created by zhiya.zhang
 * on 2022/2/17 16:00.
 */
class CameraViewModel : ViewModel() {
    //控制开关
    val mState = MutableLiveData<String>()
    val connState = MutableLiveData<Boolean>()

    //上传状态
    val sendState = MutableLiveData<String>()

    //Socket返回的结果
    val socketMsg = MutableLiveData<String>()

    val updateState = MutableLiveData<Boolean>()

    //待发送数据
    private val waitSendData = LinkedList<Uri>()

    //0=用1的url 1=用2的url
    private var sendUrl = 0

    private lateinit var socketTool: SocketTool

    private lateinit var job: Job

    val picCount = MutableLiveData<Int>()
    val sendCount = MutableLiveData<Int>()

    /**
     * 链接socket
     */
    fun connectSocket() {
        socketTool = SocketTool(viewModelScope)
        socketTool.connect(connState, socketMsg, mState)
        job = viewModelScope.launch {
            jobRun()
        }
    }

    fun tryConn() {
        socketTool = SocketTool(viewModelScope)
        socketTool.connect(connState, socketMsg, mState)
    }

    fun sendFile(uri: Uri, light: String, barCode: String, phoneType: String, zp: String, group: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val file = uri.toFile()
                    val data = file.readBytes()
                    val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                    val requestBody = data.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                    builder.addFormDataPart("file", file.name, requestBody)
                    builder.addFormDataPart("phone_type", phoneType)
                    builder.addFormDataPart("lighting_type", light)
                    builder.addFormDataPart("item_code_str", barCode)
                    when(zp) {
                        "1"  -> {
                            builder.addFormDataPart("upload_type", "0")
                        }

                        "2"  -> {
                            builder.addFormDataPart("upload_type", "1")
                            builder.addFormDataPart("group", group)
                        }

                        else -> {
                            builder.addFormDataPart("upload_type", "1")
                        }
                    }
                    val parts = builder.build().parts
                    picCount.postValue((picCount.value ?: 0) + 1)
                    val result = getHttpData(parts, zp)
                    sendCount.postValue((sendCount.value ?: 0) + 1)
                    result.takeIf { it.statusCode == 200000 }?.let {
                        Log.e("sendFile", "上传成功:" + it.statusMessage)
                    } ?: sendState.postValue("HTTP图片异常:${result.statusMessage}")
                }
            } catch(e: Exception) {
                Log.e("sendFile", "图片发送异常", e)
                sendState.postValue("HTTP图片异常:${e.message.takeIf { it.isNullOrEmpty() } ?: e.localizedMessage.takeIf { it.isNullOrEmpty() } ?: e.toString()}")
            }
        }
    }

    private suspend fun getHttpData(parts: List<MultipartBody.Part>, zp: String, tryCount: Int = 5): HttpResult {
        return try {
            if(zp == "3") {
                Api.imgService1.postImgAsync(parts).run { await() }
            } else {
                getApi().postImgAsync(parts).run { await() }
            }
        } catch(e: Exception) {
            if(tryCount == 0) {
                throw e
            } else {
                delay(1000)
                getHttpData(parts, zp, tryCount - 1)
            }
        }
    }

    private fun getApi(): APIProxy.FileAPI {
        return if(sendUrl == 0) {
            sendUrl = 1
            Api.imgService1
        } else {
            sendUrl = 0
            Api.imgService2
        }
    }

    fun sendFile(uri: Uri) {
        waitSendData.offer(uri)
    }

    private suspend fun jobRun() {
        do {
            try {
                delay(100)
                waitSendData.poll()?.also {
                    sendState.postValue("1")
                    socketTool.sendFile(it)
                    Log.e("CameraViewModel", "发送完成")
                    sendState.postValue("0")
                }
            } catch(e: CancellationException) {
                Log.e("VM", "job信息:${job.isCancelled}-${job.isActive}-${job.isCompleted}", e)
            } catch(e: Exception) {
                Log.e("CameraViewModel", "发送异常", e)
            }
        } while(!job.isCancelled && job.isActive)
    }

    fun updateApk() {
        viewModelScope.launch {
            try {
                val download = DownloadUtil({
                                                Log.e("CameraViewModel", "下载完成")
                                                updateState.value = true
                                            }, {
                                                Log.e("CameraViewModel", it)
                                            })
                download.downloadAPK()
            } catch(e: Exception) {
                Log.e("CameraViewModel", "安装异常", e)
            }
        }

    }
}