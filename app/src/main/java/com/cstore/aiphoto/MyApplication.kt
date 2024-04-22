package com.cstore.aiphoto

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.provider.Settings
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

/**
 * Created by zhiya.zhang
 * on 2022/3/2 17:27.
 */
class MyApplication : Application() {
    companion object {
        private var instance: MyApplication? = null
        @JvmStatic
        fun instance() = instance!!

        /**
         * 获得手机序列号
         */
        @SuppressLint("HardwareIds")
        @JvmStatic
        fun getOnlyId(): String {
            return Settings.Secure.getString(instance().contentResolver, Settings.Secure.ANDROID_ID)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        //全局错误信息收集
        Thread.setDefaultUncaughtExceptionHandler(GlobalException.instance())
    }
}