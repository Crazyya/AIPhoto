package com.cstore.aiphoto

import android.app.Application
import android.content.Context

/**
 * Created by zhiya.zhang
 * on 2022/3/2 17:27.
 */
class MyApplication : Application() {
    companion object {
        private var instance: MyApplication? = null
        @JvmStatic
        fun instance() = instance!!
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        //全局错误信息收集
        Thread.setDefaultUncaughtExceptionHandler(GlobalException.instance())
    }
}