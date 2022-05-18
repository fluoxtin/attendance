package com.example.attendance.model

import java.io.Serializable
import java.util.*

data class AttendTask(
    val attend_id : String,
    val cour_id : String,
    val location: Location,
    val deadline : Long
) : Serializable