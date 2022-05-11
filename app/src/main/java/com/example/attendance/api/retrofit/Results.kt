package com.example.attendance.api.retrofit

data class Results<Any> (
    val msg : String? = null,
    val code : Int? = null,
    val data : Any? = null
)