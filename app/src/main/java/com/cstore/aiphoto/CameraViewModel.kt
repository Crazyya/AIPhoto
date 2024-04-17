package com.cstore.aiphoto

import android.graphics.BitmapFactory
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
import java.io.FileInputStream
import java.util.*
import kotlin.collections.ArrayList

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
    private val waitSendData1 = LinkedList<SendData>()
    private val waitSendData2 = LinkedList<SendData>()

    private data class SendData(
        val uri: Uri, val light: String, val barCode: String, val phoneType: String, val zp: String, val group: String, val tzid: String
                               )

    //0=用1的url 1=用2的url
    private var sendUrl = 0

    private lateinit var socketTool: SocketTool

    private var job1: Job? = null
    private var job2: Job? = null

    val picCount = MutableLiveData<Int>()
    val sendCount = MutableLiveData<Int>()

    /**
     * 链接socket
     */
    fun connectSocket(ip: String) {
        socketTool = SocketTool(viewModelScope, ip, socketMsg, mState)
        socketTool.connect(connState)
    }

    fun tryConn(ip: String) {
        socketMsg.postValue("")
        socketTool = SocketTool(viewModelScope, ip, socketMsg, mState)
        socketTool.connect(connState)
    }

    private var waitDataJud = 0

    fun sendFile(uri: Uri, light: String, barCode: String, phoneType: String, zp: String, group: String, tzid: String) {
        if(waitDataJud == 0) {
            waitSendData1.offer(SendData(uri, light, barCode, phoneType, zp, group, tzid))
            waitDataJud = 1
            if(job1 == null || (job1!!.isCancelled && !job1!!.isActive)) {
                job1 = viewModelScope.launch { sendJobRun1() }
            }
        } else {
            waitSendData2.offer(SendData(uri, light, barCode, phoneType, zp, group, tzid))
            waitDataJud = 0
            if(job2 == null || (job2!!.isCancelled && !job2!!.isActive)) {
                job2 = viewModelScope.launch { sendJobRun2() }
            }
        }
    }

    private suspend fun sendJobRun1() = withContext(Dispatchers.IO) {
        while(true) {
            try {
                if(waitSendData1.isEmpty()) {
                    delay(1000)
                    continue
                }
                waitSendData1.poll()?.also { sendData ->
                    val file = sendData.uri.toFile()
                    val inst = FileInputStream(file)
                    val bmp = BitmapFactory.decodeStream(inst)
                    inst.close()
                    try {
                        val data = file.readBytes()
                        val x = bmp.byteCount
                        print(x)
                        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                        val requestBody = data.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                        builder.addFormDataPart("file", file.name, requestBody)
                        builder.addFormDataPart("phone_type", sendData.phoneType)
                        builder.addFormDataPart("lighting_type", sendData.light)
                        builder.addFormDataPart("item_code_str", sendData.barCode)
                        if(sendData.tzid.isNotEmpty()) {
                            builder.addFormDataPart("sampling_task_id", sendData.tzid)
                        }
                        val uploadType = when(sendData.zp) {
                            "1"  -> {
                                "0"
                            }

                            "2"  -> {
                                builder.addFormDataPart("group", sendData.group)
                                "1"
                            }

                            "3"  -> {
                                "2"
                            }

                            else -> {
                                "1"
                            }
                        }
                        builder.addFormDataPart("upload_type", uploadType)
                        val parts = builder.build().parts
                        val result = getHttpData(parts, sendData.zp)
                        sendCount.postValue((sendCount.value ?: 0) + 1)
                        result.takeIf { it.statusCode == 200000 }?.let {
                            Log.e("sendFile", "上传成功:" + it.statusMessage)
                        } ?: sendState.postValue("HTTP图片异常:${result.statusMessage}")
                    } finally {
                        file.delete()
                    }
                }
            } catch(e: CancellationException) {
                Log.e("VM", "job信息:${job1!!.isCancelled}-${job1!!.isActive}-${job1!!.isCompleted}", e)
            } catch(e: Exception) {
                Log.e("CameraViewModel", "发送异常", e)
            }
        }
    }

    private suspend fun sendJobRun2() = withContext(Dispatchers.IO) {
        while(true) {
            try {
                if(waitSendData2.isEmpty()) {
                    delay(1000)
                    continue
                }
                waitSendData2.poll()?.also { sendData ->
                    val file = sendData.uri.toFile()
                    try {
                        val data = file.readBytes()
                        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                        val requestBody = data.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                        builder.addFormDataPart("file", file.name, requestBody)
                        builder.addFormDataPart("phone_type", sendData.phoneType)
                        builder.addFormDataPart("lighting_type", sendData.light)
                        builder.addFormDataPart("item_code_str", sendData.barCode)
                        if(sendData.tzid.isNotEmpty()) {
                            builder.addFormDataPart("sampling_task_id", sendData.tzid)
                        }
                        val uploadType = when(sendData.zp) {
                            "1"  -> {
                                "0"
                            }

                            "2"  -> {
                                builder.addFormDataPart("group", sendData.group)
                                "1"
                            }

                            "3"  -> {
                                "2"
                            }

                            else -> {
                                "1"
                            }
                        }
                        builder.addFormDataPart("upload_type", uploadType)
                        val parts = builder.build().parts
                        val result = getHttpData(parts, sendData.zp)
                        sendCount.postValue((sendCount.value ?: 0) + 1)
                        result.takeIf { it.statusCode == 200000 }?.let {
                            Log.e("sendFile", "上传成功:" + it.statusMessage)
                        } ?: sendState.postValue("HTTP图片异常:${result.statusMessage}")
                    } finally {
                        file.delete()
                    }
                }
            } catch(e: CancellationException) {
                Log.e("VM", "job信息:${job2!!.isCancelled}-${job2!!.isActive}-${job2!!.isCompleted}", e)
            } catch(e: Exception) {
                Log.e("CameraViewModel", "发送异常", e)
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
//        return Api.imgService2
        return if(sendUrl == 0) {
            sendUrl = 1
            Api.imgService1
        } else {
            sendUrl = 0
            Api.imgService2
        }
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