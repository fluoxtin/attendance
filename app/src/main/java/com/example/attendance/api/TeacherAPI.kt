package com.example.attendance.api

import com.example.attendance.api.retrofit.Results
import com.example.attendance.model.*
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.POST

interface TeacherAPI {

    @POST("teacher/login")
    fun registerOrLoginForTea(@Body user: User) : Observable<Results<Teacher>>

    @POST("teacher/update")
    fun updateTeacherInfo(@Body teacher: Teacher) : Observable<Results<Teacher>>

    @POST("teacher/getInfo")
    fun getTeacherInfo() : Observable<Results<Teacher>>

    @POST("teacher/getcourses")
    fun getCourses() : Observable<Results<List<Course>>>

    @POST("teacher/posttask")
    fun postTask(
        @Body task : AttendTask
    ) : Observable<Results<Any>>

    @POST("teacher/getrecord")
    fun getRecordForT() : Observable<Results<List<CourseAttendanceRecord>>>

    @POST("teacher/getallrecords")
    fun getStuRecords(@Body record : CourseAttendanceRecord) : Observable<Results<List<StudentRecord>>>

    @POST("teacher/getcurtask")
    fun getCurTask() : Observable<Results<AttendTask>>

}