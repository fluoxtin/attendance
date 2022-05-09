package com.example.attendance.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * @author create by fluoxtin on 20222/5/8
 *
 */

data class Student(
    @SerializedName("stu_id")
    var stu_id : String?,
    @SerializedName("name")
    var name: String?,
    @SerializedName("sex")
    var sex : String?,
    @SerializedName("phone")
    var phone : String?,
    @SerializedName("email")
    var email : String?,
    @SerializedName("unit")
    var unit : String?,
    @SerializedName("stu_class")
    var stu_class : String?,
    @SerializedName("major")
    var major : String?
) : Serializable
