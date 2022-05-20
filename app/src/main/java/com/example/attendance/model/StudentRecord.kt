package com.example.attendance.model

data class StudentRecord(
    val atten_id : String,
    val stu_id : String,
    val stu_name : String,
    val attendance : Int,
    val sign_in_time : Long
)
