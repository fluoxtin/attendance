package com.example.attendance.api

import com.example.attendance.api.retrofit.Results
import com.example.attendance.model.AttendTask
import com.example.attendance.model.Course
import com.example.attendance.model.Student
import com.example.attendance.model.User
import io.reactivex.Observable
import retrofit2.http.*

interface StudentAPI {

    @POST("student/login")
    fun registerOrLoginForStu(
        @Body user: User
    ) : Observable<Results<Student>>

    @POST("student/update")
    fun updateStudentInfo(
        @Header("token") token : String,
        @Body student: Student
    ) : Observable<Results<Student>>

    @POST("student/getinfo")
    fun getStudentInfo() : Observable<Results<Student>>

    @POST("student/getcourses")
    fun getCourses() : Observable<Results<List<Course>>>

    @POST("student/gettask")
    fun getTask() : Observable<Results<AttendTask>>

}