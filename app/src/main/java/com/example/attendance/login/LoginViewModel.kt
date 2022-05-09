package com.example.attendance.login

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.attendance.App
import com.example.attendance.R
import com.example.attendance.model.Student
import com.example.attendance.model.Teacher
import com.example.attendance.model.User
import com.example.attendance.retrofit.Results
import com.example.attendance.retrofit.RetrofitManager
import com.example.attendance.util.SharedPreferencesUtils
import com.google.gson.Gson
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginViewModel() : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState : LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult : LiveData<LoginResult> = _loginResult

    fun login(username : String, password : String, role : Int) {



        val apiService = RetrofitManager.getService(APIService::class.java)
        val user = User(username, password, role)

        if (role == 0) {
            apiService.registerOrLoginForTea(user)
                .enqueue(object : Callback<Results<Teacher>> {
                    override fun onResponse(
                        call: Call<Results<Teacher>>,
                        response: Response<Results<Teacher>>
                    ) {
                        Log.i(TAG, "onResponse: $response")
                        response.body()?.apply {
                            token?.let { SharedPreferencesUtils.putToken(it) }
                            if (success == true) {
                                val teacher = data
                                if (teacher == null)
                                    _loginResult.value = LoginResult(needRegister = true)
                                else{
                                    _loginResult.value = LoginResult(success = message)
                                    SharedPreferencesUtils.putCurrentTeacher(teacher)
                                }
                                SharedPreferencesUtils.putCurrentUser(User(username, password, role))
                            } else {
                                _loginResult.value = LoginResult(error = response.code())
                            }
                        }
                    }

                    override fun onFailure(call: Call<Results<Teacher>>, t: Throwable) {
                        Log.i(TAG, "onFailure: ${t.message}")
                    }
                })
        } else {
            apiService.registerOrLoginForStu(user)
                .enqueue(object : Callback<Results<Any>> {
                override fun onResponse(
                    call: Call<Results<Any>>,
                    response: Response<Results<Any>>
                ) {
                    Log.i(TAG, "onResponse: $response")
                    response.body()?.apply {
                        token?.let { SharedPreferencesUtils.putToken(it) }
                        if (success == true) {
                            Log.d(TAG, "onResponse: ${response.body().toString()}")
                            val student = data
                            if (student == null)
                                _loginResult.value = LoginResult(needRegister = true)
                            else {
                                _loginResult.value = LoginResult(success = message)
                                SharedPreferencesUtils.putCurrentStudent(student as Student)
                            }
                            SharedPreferencesUtils.putCurrentUser(User(username, password, role))
                        } else {
                            _loginResult.value = LoginResult(error = response.code())
                        }
                    }
                }

                override fun onFailure(call: Call<Results<Any>>, t: Throwable) {
                    Log.i(TAG, "onFailure: ${t.message}")
                }

            })
        }
    }

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    private fun isUserNameValid(username: String) : Boolean {
        return if (username.contains('@')) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    private fun isPasswordValid(password: String) : Boolean {
        return password.length > 5
    }

    companion object {
        const val TAG = "LoginViewModel"
    }

}