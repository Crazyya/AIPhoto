package com.cstore.aiphoto

import android.app.Activity
import android.os.Environment
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.net.*
import kotlin.system.exitProcess

/**
 * Created by zhiya.zhang
 * on 2018/3/13 16:17.
 * 全局错误信息收集
 *
 */
class GlobalException private constructor() : Thread.UncaughtExceptionHandler {
    private val mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    companion object {
        private val jobCancellExceptionName = "JobCancellationException"

        @JvmStatic
        val crashFileName = "CStoreAIErrorMessage.txt"

        private var instance: GlobalException? = null

        fun instance(): GlobalException {
            if (instance == null) {
                synchronized(GlobalException::class.java) {
                    if (instance == null) {
                        instance = GlobalException()
                    }
                }
            }
            return instance!!
        }

        /**
         * 记录错误信息
         */
        fun saveErrorInfoFile(ex: Throwable, msg: String? = null) {
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                val writer = StringWriter()
                val printWriter = PrintWriter(writer)
                ex.printStackTrace(printWriter)
                printWriter.close()
                val result = "$msg $writer"
                val outputStream = MyApplication.instance().applicationContext.openFileOutput(
                    crashFileName,
                    Activity.MODE_APPEND
                )
                outputStream.write(result.toByteArray())
                outputStream.flush()
                outputStream.close()
            }
        }
    }

    override fun uncaughtException(t: Thread?, ex: Throwable?) {
        try {
            ex.takeIf { handleException(it) }?.also { throwable ->
                mDefaultHandler?.also { handler ->
                    t?.also {
                        handler.uncaughtException(it, throwable)
                    }
                }
            }
            exitProcess(1)
        } catch (e: Exception) {
            exitProcess(1)
        }
    }

    private fun handleException(ex: Throwable?): Boolean {
        ex ?: return false
        ex::class.java.name.takeIf { jobCancellExceptionName !in it }?.also {
                saveErrorInfoFile(ex)
                GlobalScope.launch { ReportListener.reportError(true) }
            }
        return true
    }

}