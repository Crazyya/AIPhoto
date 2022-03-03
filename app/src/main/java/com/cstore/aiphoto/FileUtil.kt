package com.cstore.aiphoto

import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * Created by zhiya.zhang
 * on 2022/3/3 9:13.
 */
object FileUtil {
    /**
     * 安装apk
     * @param apkFile apk路径
     * @param authority 在应用清单的<provider>元素中定义的FileProvider的权限
     */
    fun installApk(apkFile: File, authority: String) {
        if (authority.isEmpty()) return
        if (apkFile.toString().substring(apkFile.toString().length - 3) != "apk") {
            return
        }
        val file = apkFile.takeIf { it.exists() } ?: return
        Log.e("FileUtil", "fileName:${file.name};fileObPath:${file.absolutePath}")
        val uri = FileProvider.getUriForFile(MyApplication.instance().applicationContext, authority, file)
        try {
            uri?.also {
                val i = Intent(Intent.ACTION_VIEW)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                i.setDataAndType(uri, "application/vnd.android.package-archive")
                MyApplication.instance().applicationContext.startActivity(i)
            }
        } catch (e: Exception) {
            throw Exception("安装异常$e")
        }
    }
}