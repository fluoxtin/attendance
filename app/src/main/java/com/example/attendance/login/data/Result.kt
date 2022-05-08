package com.example.attendance.login.data

import java.lang.Exception

/**
 * @author: fluoxtin created on 2022/4/23
 */
sealed class Result<out T : Any> {

    data class Success<out T : Any>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()

    override fun toString(): String {
        return when(this) {
            is Success<*> -> "SUccess[data=$data]"
            is Error -> "Error[exception=$exception]"
        }
    }
}