package com.cstore.aiphoto

import android.app.DownloadManager
import android.app.DownloadManager.STATUS_SUCCESSFUL
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Created by zhiya.zhang
 * on 2022/3/2 10:32.
 */
class DownloadUtil(private val succ: ((String) -> Unit), private val err: ((String) -> Unit)) {

    companion object {
        private const val FILE_NAME = "AIPhoto.apk"
        val filePath = File(MyApplication.instance().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absoluteFile, FILE_NAME)
        val apkFilePath = File(MyApplication.instance().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), FILE_NAME).absoluteFile
        private const val DOWNLOAD_URL = "https://cstorecloud.oss-cn-shanghai.aliyuncs.com/apk/AIImage_apk/app-release.apk"
    }

    suspend fun downloadAPK() = withContext(Dispatchers.IO) {
        val file = filePath
        if (file.exists()) {
            file.delete()
        }
        val request = DownloadManager.Request(Uri.parse(DOWNLOAD_URL)).setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE).setTitle("下载").setDescription("正在下载$FILE_NAME").setAllowedOverRoaming(true)
                .setDestinationInExternalFilesDir(MyApplication.instance(), Environment.DIRECTORY_DOWNLOADS, FILE_NAME)
        val downManger = MyApplication.instance().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = downManger.enqueue(request)
        val completeReceiver = CompleteReceiver(id) {
            Log.e("Download", "Download Done!")
            succ(file.absolutePath)
        }
        ContextCompat.registerReceiver(
            MyApplication.instance(),
            completeReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        var isDownload: Int
        do {
            delay(100)
            isDownload = getBytesAndStatus(id, downManger)[2]
            Log.e("Download", "State:$isDownload DoneState:${STATUS_SUCCESSFUL}")
        } while (isDownload != STATUS_SUCCESSFUL)
        MyApplication.instance().unregisterReceiver(completeReceiver)
    }

    private suspend fun getBytesAndStatus(downloadId: Long, downManger: DownloadManager): IntArray = withContext(Dispatchers.IO) {
        val byteAndStatus = intArrayOf(-1, -1, 0)
        val query: DownloadManager.Query = DownloadManager.Query().setFilterById(downloadId)
        var cursor: Cursor? = null
        try {
            cursor = downManger.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                //已经下载大小
                byteAndStatus[0] = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                //下载文件总大小
                byteAndStatus[1] = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                //下载状态
                byteAndStatus[2] = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS).takeIf { it >= 0 } ?: 0)
            }
        } catch (e: Exception) {

        } finally {
            cursor?.close()
        }
        byteAndStatus
    }

    private inner class CompleteReceiver(val downloadId: Long, val result: (BroadcastReceiver) -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val completeDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (completeDownloadId == downloadId) {
                result(this)
            }
        }
    }
}