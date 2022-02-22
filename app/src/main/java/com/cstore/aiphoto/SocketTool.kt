package com.cstore.aiphoto

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI

/**
 * Created by zhiya.zhang
 * on 2022/2/17 16:04.
 */
class SocketTool(private var scope: CoroutineScope) {
    private val TRY_COUNT = 5
    private val CONNECT_TIME_OUT = 1500
    private val HOST = "192.168.7.88"
    private val PORT = 52828
    private val FILE_PORT = 52827
    private lateinit var job: Job

    fun connect(conn: MutableLiveData<Boolean>, msg: MutableLiveData<String>, state: MutableLiveData<String>) {
        job = scope.launch {
            try {
                conn.postValue(true)
                connectAndLife(msg, state)
            } catch (e: Exception) {
                conn.postValue(false)
                Log.e("SocketTool", "连接异常", e)
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun connectAndLife(msg: MutableLiveData<String>, state: MutableLiveData<String>, tryCount: Int = TRY_COUNT): Boolean = withContext(Dispatchers.IO) {
        val socket = Socket().also {
            it.setSoLinger(true, 0)
            it.keepAlive = true
            it.connect(InetSocketAddress(HOST, PORT), CONNECT_TIME_OUT)
        }
        msg.postValue("Succ!")
        val os = socket.getOutputStream()
        val ins = socket.getInputStream()
        try {
            do {
                var text: String? = null
                //发送心跳包,直到接受到开始指令,之后就是等待拍照发送完成继续心跳包
                while (text.isNullOrEmpty()) {
                    os.write("hi".toByteArray())
                    os.flush()
                    //无限，我自己写的服务端，要不断开，要不返回结果
                    text = BufferedReader(InputStreamReader(ins, Charsets.UTF_8)).readLine()
                    Log.e("SocketTool", "接受:$text")
                }
                if (text == "start") {
                    state.postValue(text)
                    //重置
                    delay(500)
                    state.postValue("")
                }
                delay(500)
            } while (!job.isCancelled && job.isActive)
        } catch (e: SocketTimeoutException) {
            Log.e("SocketTool", "连接超时,准备重试:${tryCount}", e)
            if (tryCount <= 0) {
                msg.postValue("try err!connect close!")
                throw e
            } else {
                msg.postValue("try connect")
                connectAndLife(msg, state, tryCount - 1)
            }
        } catch (e: Exception) {
            msg.postValue("err!connect close!$e")
            throw e
        } finally {
            Log.e("SocketTool", "连接断开")
            os.close()
            ins.close()
            socket.close()
        }
        true
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun sendFile(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        Log.e("SocketToolFile", "sendStart")
        val fileSocket = Socket().also {
            it.connect(InetSocketAddress(HOST, FILE_PORT), CONNECT_TIME_OUT)
            it.setSoLinger(true, 0)
        }
        val fileOs = fileSocket.getOutputStream()
        val fileIns = fileSocket.getInputStream()
        try {
            val file = uri.toFile()
            val fileByte = file.readBytes()
            val fileName = file.name
            ByteArrayOutputStream().use { bas ->
                val bitmap = BitmapFactory.decodeByteArray(fileByte, 0, fileByte.size)
                val isCompress = bitmap.compress(Bitmap.CompressFormat.JPEG, 60, bas)
                val bytes = if (isCompress) {
                    Log.e("SocketToolFile", "compress succ")
                    bas.toByteArray()
                } else {
                    Log.e("SocketToolFile", "compress err")
                    fileByte
                }
                bas.flush()
                val fileSize = bytes.size
                fileOs.write("fileput $fileSize $fileName".toByteArray())
                delay(200)
                fileOs.write(bytes)
                fileOs.flush()
            }
            var text: String?
            do {
                text = BufferedReader(InputStreamReader(fileIns, Charsets.UTF_8)).readLine()
            } while (text.isNullOrEmpty())
            Log.e("SocketToolFile", "sendResult:${text}")
        } catch (e: Exception) {
            Log.e("SocketToolFile", "sendError", e)
            sendFile(uri)
        } finally {
            Log.e("SocketToolFile", "sendEnd")
            fileOs.close()
            fileIns.close()
            fileSocket.close()
        }
        true
    }
}