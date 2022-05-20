package com.example.attendance.ui.report

import com.example.attendance.model.CourseAttendanceRecord

interface OnItemClickListener {
    fun onClick(record : CourseAttendanceRecord)
}