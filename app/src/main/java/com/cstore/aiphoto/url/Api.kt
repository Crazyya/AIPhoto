package com.cstore.aiphoto.url

/**
 * Created by zhiya.zhang
 * on 2022/10/10 10:37.
 */
class Api private constructor() : BaseApi() {
    companion object {
        private val api = Api()
        val imgService1: APIProxy.FileAPI = api.initRetrofit(APIProxy.API_BASE_URL1).create(APIProxy.FileAPI::class.java)
        val imgService2: APIProxy.FileAPI = api.initRetrofit(APIProxy.API_BASE_URL2).create(APIProxy.FileAPI::class.java)
    }
}