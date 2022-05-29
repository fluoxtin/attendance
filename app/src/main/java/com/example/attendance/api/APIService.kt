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

    @POST("/api/islogin")
    fun isLogin() : Observable<Results<User>>

    @POST("/api/delete")
    fun delete() : Observable<Results<Any>>

}