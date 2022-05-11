package com.example.attendance.api.retrofit

import android.util.Log
import com.example.attendance.util.SharedPreferencesUtils
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory


object  RetrofitManager {

    const val TAG = "RetrofitManager"

    private const val BASE_URL = "http://39.101.128.27:8802/api/"

    private val headInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("token", SharedPreferencesUtils.getToken())
            .build()
        val response = chain.proceed(request)
        val header = response.headers()
            header.get("token")?.apply {
                Log.i(TAG, "token: $this")
            }

        val token = response.headers().get("token")?.substringAfter("token: ")

        token?.let {
            SharedPreferencesUtils.putToken(it)
        }
        response
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(headInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()

    fun <T> getService(clazz: Class<T>?): T {
        return retrofit.create(clazz)
    }

}