package com.example.attendance.login.data

import android.util.Log
import com.example.attendance.login.APIService
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.*

/**
 * @author: fluoxtin created on 2022/4/23
 */
class LoginDataSource {

    fun login(username: String, password: String, role : Int) : Result<LoggedInUser> {
        try {
            // TODO: login or register
            val mediaType = MediaType.parse("text/plain")
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("username", username)
                    .addFormDataPart("password", password)
                .addFormDataPart("role", role.toString())
                    .build()


            val retrofit = Retrofit.Builder()
                .baseUrl("http://39.101.128.27:8802")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val apiService = retrofit.create(APIService::class.java)

            Thread {
                val responseBody = apiService.registerOrLogin(body)
                val date = responseBody.execute().body()
                Log.i(TAG, "login: ${date.toString()}")
            }.start()

            

            val user = LoggedInUser(UUID.randomUUID().toString(), username, role)
            return Result.Success(user)
        } catch (e : Throwable) {
            return Result.Error(IOException("Error logging in", e))
        }
    }

    fun logout() {

    }

    companion object {
        const val TAG = "LoginDataSource"
    }
}