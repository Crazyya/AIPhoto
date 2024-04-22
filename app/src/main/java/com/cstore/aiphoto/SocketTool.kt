package com.cstore.aiphoto

import android.graphics.*
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.lifecycle.MutableLiveData
import com.cstore.aiphoto.DownloadUtil.Companion.filePath
import kotlinx.coroutines.*
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Created by zhiya.zhang
 * on 2022/2/17 16:04.
 */
class SocketTool(
    private var scope: CoroutineScope,
    private var host: String,
    private val msg: MutableLiveData<String>,
    private val state: MutableLiveData<String>
) {
    private val CONNECT_TIME_OUT = 1500
    private val PORT = 52828
    private lateinit var job: Job

    fun connect(conn: MutableLiveData<Boolean>) {
        job = scope.launch {
            try {
                conn.postValue(true)
                connectAndLife()
            } catch (e: Exception) {
                delay(1000)
                conn.postValue(false)
                Log.e(
                    "SocketTool", "连接异常", e
                )
            }
        }
    }

    private suspend fun connectAndLife(): Boolean = withContext(Dispatchers.IO) {
        val socket = Socket().also {
            it.setSoLinger(
                true, 0
            )
            it.keepAlive = true
            it.connect(
                InetSocketAddress(
                    host, PORT
                ), CONNECT_TIME_OUT
            )
        }
        msg.postValue("Succ!")
        val os = socket.getOutputStream()
        val ins = socket.getInputStream()
        try {
            val androidId = MyApplication.getOnlyId()
            //发一次确认通了就行了
            os.write("device_name${androidId}".toByteArray())
            os.flush()
            do {
                var text: String? = null
                while (text.isNullOrEmpty()) {
                    //无限，我自己写的服务端，要不断开，要不返回结果
                    text = BufferedReader(
                        InputStreamReader(
                            ins, Charsets.UTF_8
                        )
                    ).readLine()
                }
                if ("apkfile" in text) {
                    val fileSize = text.split(" ")[1].toLong()
                    val fos = FileOutputStream(filePath)
                    val bufSize = 1024
                    val buf = ByteArray(bufSize)
                    val bis = BufferedInputStream(ins)
                    var len: Int
                    var totalSize = 0
                    Log.e(
                        "SocketTool", "开始接收"
                    )
                    while ((bis.read(buf)).also { len = it } != -1) {
                        fos.write(
                            buf, 0, len
                        )
                        totalSize += len
                        if (fileSize <= totalSize) {
                            Log.e(
                                "SocketTool", "接收长度满足，结束"
                            )
                            break
                        }
                    }
                    fos.close()
                    Log.e(
                        "SocketTool", "接收完毕"
                    )
                    state.postValue("update")
                } else if (!text.isNullOrEmpty() && text != "1") {
                    state.postValue(text!!)
                    //重置
                    delay(500)
                    state.postValue("")
                }
            } while (!job.isCancelled && job.isActive)
        } catch (e: Exception) {
            msg.postValue("err! connect close!$e")
            throw e
        } finally {
            Log.e(
                "SocketTool", "连接断开"
            )
            ins.close()
            os.close()
            socket.close()
        }
        true
    }
}