package com.example.attendance.model

data class AttendanceRecord(
    val attend_id : String,
    val cour_name : String,
    val sign_in_time : Long,
    val isAttendance : Int)
