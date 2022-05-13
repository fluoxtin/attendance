package com.example.attendance.api

import com.example.attendance.api.retrofit.Results
import com.example.attendance.model.AttendTask
import com.example.attendance.model.Course
import com.example.attendance.model.Teacher
import com.example.attendance.model.User
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.Header
import retrofit2.http.POST

interface TeacherAPI {

    @POST("teacher/login")
    fun registerOrLoginForTea(@Body user: User) : Observable<Results<Teacher>>

    @POST("teacher/update")
    fun updateTeacherInfo(
        @Header("token") token: String,
        @Body teacher: Teacher
    ) : Observable<Results<Teacher>>

    @POST("teacher/getInfo")
    fun getTeacherInfo() : Observable<Results<Teacher>>

    @POST("teacher/getcourses")
    fun getCourses() : Observable<Results<List<Course>>>

    @POST("teacher/posttask")
    fun postTask(
        @Body task : AttendTask
    ) : Observable<Results<Any>>

}