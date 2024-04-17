package com.cstore.aiphoto.url

import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Created by zhiya.zhang
 * on 2022/10/10 9:42.
 */
abstract class BaseApi {
    fun initRetrofit(baseUrl: String): Retrofit {
        val timeOutSecond: Long = 5
        val CONNECTION_HEADER = "Connection"
        val CONNECTION_SWITCH = "close"
        val builder = OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .addInterceptor {
                    val original = it.request()
                    val request = original.newBuilder()
                            .header(CONNECTION_HEADER, CONNECTION_SWITCH)
                            .method(original.method, original.body)
                            .build()
                    it.proceed(request)
                }
                .connectTimeout(timeOutSecond, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
        val client = builder.build()
        val retrofit = Retrofit.Builder().apply {
            baseUrl(baseUrl)
            client(client)
            addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            addCallAdapterFactory(CoroutineCallAdapterFactory())
        }
        return retrofit.build()
    }
}