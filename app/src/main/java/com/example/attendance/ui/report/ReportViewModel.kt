package com.example.attendance.ui.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.attendance.model.AttendanceRecord
import com.example.attendance.util.SharedPreferencesUtils

class ReportViewModel : ViewModel() {

    private var _stuAttendRecords = MutableLiveData<List<AttendanceRecord>>()
    val stuAttendRecords : LiveData<List<AttendanceRecord>> = _stuAttendRecords

    init {
        val role = SharedPreferencesUtils.getCurrentUserRole()
        when(role) {
            0 -> getCourseRecord()
        }
    }

    fun getCourseRecord() {

    }


}