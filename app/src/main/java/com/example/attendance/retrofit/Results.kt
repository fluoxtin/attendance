package com.example.attendance.retrofit

import com.example.attendance.model.Student
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Results<T>(
    var message : String? = null,
    var success : Boolean? = null,
    var token : String? = null,
    var data : T? = null
) : Serializable
