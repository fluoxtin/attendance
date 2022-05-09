package com.example.attendance.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class User(
    @SerializedName("username")
    var username : String,
    @SerializedName("password")
    var password : String,
    @SerializedName("role")
    var role : Int
) : Serializable