package com.example.attendance.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Teacher(
    @SerializedName("tea_id")
    var username : String,
    @SerializedName("tea_name")
    var tea_name : String,
    @SerializedName("sex")
    var sex : String,
    @SerializedName("phone")
    var phone : String,
    @SerializedName("email")
    var email : String,
    @SerializedName("unit")
    var unit : String
) : Serializable
