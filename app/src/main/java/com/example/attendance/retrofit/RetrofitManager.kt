package com.example.attendance.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object  RetrofitManager {

    private const val BASE_URL = "http://39.101.128.27:8802/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun <T> getService(clazz: Class<T>?): T {
        return retrofit.create(clazz)
    }

}