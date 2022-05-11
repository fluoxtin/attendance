package com.example.attendance.api

import com.example.attendance.model.Student
import com.example.attendance.model.Teacher
import com.example.attendance.model.User
import com.example.attendance.api.retrofit.Results
import io.reactivex.Observable
import retrofit2.http.*

/**
 * @author: fluoxtin created on 2022/4/24
 */
interface APIService {


    @POST("teacher/login")
    fun registerOrLoginForTea(@Body user: User) : Observable<Results<Teacher>>

    @POST("student/login")
    fun registerOrLoginForStu(
        @Body user: User
    ) : Observable<Results<Student>>

    @POST("student/update")
    fun updateStudentInfo(
        @Header("token") token : String,
        @Body student: Student
    ) : Observable<Results<Student>>

    @POST("teacher/update")
    fun updateTeacherInfo(
        @Header("token") token: String,
        @Body teacher: Teacher
    ) : Observable<Results<Teacher>>

    @POST("islogin")
    fun isLogin() : Observable<Results<Any>>
}