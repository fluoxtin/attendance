package com.example.attendance.model

import java.io.Serializable

data class CourseAttendanceRecord(
    val tea_id : String,
    val attend_id : String,
    val cour_name : String,
    val time : Long,
    val total : Int,
    val actual : Int
) : Serializable
