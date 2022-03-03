package com.cstore.aiphoto

import android.app.Application

/**
 * Created by zhiya.zhang
 * on 2022/3/2 17:27.
 */
class MyApplication : Application() {
    companion object {
        private var instance: MyApplication? = null
        fun instance() = instance!!
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}