package com.example.attendance.model

data class CourseAttendanceRecord(
    val tea_id : String,
    val attend_id : String,
    val cour_name : String,
    val time : String,
    val total_student : Int,
    val actual_attendance : Int
)
