package com.cstore.aiphoto

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
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
    val sendState = MutableLiveData<Boolean>()

    //Socket返回的结果
    val socketMsg = MutableLiveData<String>()

    val updateState = MutableLiveData<Boolean>()

    //待发送数据
    private val waitSendData = LinkedList<Uri>()

    private lateinit var socketTool: SocketTool

    private lateinit var job: Job

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

    fun sendFile(uri: Uri) {
        waitSendData.offer(uri)
    }

    private suspend fun jobRun() {
        do {
            try {
                delay(100)
                waitSendData.poll()?.also {
                    sendState.postValue(true)
                    socketTool.sendFile(it)
                    Log.e("CameraViewModel", "发送完成")
                    sendState.postValue(false)
                }
            } catch (e: CancellationException) {
                Log.e("VM", "job信息:${job.isCancelled}-${job.isActive}-${job.isCompleted}", e)
            } catch (e: Exception) {
                Log.e("CameraViewModel", "发送异常", e)
            }
        } while (!job.isCancelled && job.isActive)
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
            } catch (e: Exception) {
                Log.e("CameraViewModel", "安装异常", e)
            }
        }

    }
}