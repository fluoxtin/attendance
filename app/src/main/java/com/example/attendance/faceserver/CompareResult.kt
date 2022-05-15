package com.example.attendance.faceserver

data class CompareResult(
    val username : String? = null,
    val similar : Float,
    val trackId : Int? = null
)
