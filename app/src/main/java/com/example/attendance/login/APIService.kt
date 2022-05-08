package com.example.attendance.login

import com.example.attendance.login.data.LoggedInUser
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * @author: fluoxtin created on 2022/4/24
 */
interface APIService {

    @POST("register")
    fun register(@Part requestBodyMap : Map<String, RequestBody>) : Call<Response<Any>>?

    @POST("register")
    fun registerOrLogin(@Body requestBody: RequestBody) : Call<ResponseBody>

    @POST("student/update")
    fun updateStudentInfo(
        @Header("token") token : String,
        @Body requestBody: RequestBody
    ) : Call<ResponseBody>
}