package com.example.attendance.model

data class RequestLeave(
    val id : String,
    val msg : String,
    val start : Long,
    val end : Long
)
