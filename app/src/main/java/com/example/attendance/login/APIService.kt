package com.example.attendance.login

import com.example.attendance.model.Student
import com.example.attendance.model.Teacher
import com.example.attendance.model.User
import com.example.attendance.retrofit.Results
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

/**
 * @author: fluoxtin created on 2022/4/24
 */
interface APIService {


    @POST("login")
    fun registerOrLoginForTea(@Body user: User) : Call<Results<Teacher>>

    @POST("login")
    fun registerOrLoginForStu(
        @Body user: User
    ) : Call<Results<Any>>

    @POST("student/update")
    fun updateStudentInfo(
        @Header("token") token : String,
        @Body student: Student
    ) : Call<Results<Student>>

    @POST("teacher/update")
    fun updateTeacherInfo(
        @Header("token") token: String,
        @Body teacher: Teacher
    ) : Call<Results<Teacher>>
}