package com.example.attendance.login.data

import com.google.gson.annotations.SerializedName

/**
 * @author: fluoxtin created on 2022/4/23
 */
data class LoggedInUser(
    @SerializedName("username")
    val username : String,
    @SerializedName("password")
    val password : String,
    @SerializedName("role")
    val role : Int
)
