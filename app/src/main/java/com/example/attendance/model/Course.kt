package com.example.attendance.model

import java.io.Serializable
import java.sql.Time

data class Course(
    val cour_id : String,
    val cour_name : String,
    val tea_name : String,
    val start : String,
    val end : String,
    val week_day : Int
) : Serializable