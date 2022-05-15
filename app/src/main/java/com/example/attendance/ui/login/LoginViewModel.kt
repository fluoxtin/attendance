package com.example.attendance.ui.login

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.attendance.R
import com.example.attendance.api.StudentAPI
import com.example.attendance.api.TeacherAPI
import com.example.attendance.model.Student
import com.example.attendance.model.Teacher
import com.example.attendance.model.User
import com.example.attendance.api.retrofit.Results
import com.example.attendance.api.retrofit.RetrofitManager
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class LoginViewModel : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState : LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult : LiveData<LoginResult> = _loginResult

    fun login(username : String, password : String, role : Int) {

        val user = User(username, password, role)
        if (role == 0) {
            loginForT(user)
        } else {
           loginForS(user)
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

    private fun loginForT(user : User) {
        RetrofitManager.getService(TeacherAPI::class.java)
            .registerOrLoginForTea(user)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Results<Teacher>> {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext(t: Results<Teacher>) {
                    if (t.code == 200) {
                        if (t.data != null)
                            _loginResult.value = LoginResult(success = "true")
                        else _loginResult.value = LoginResult(success = "true", needRegister = true)
                    } else
                        _loginResult.value = LoginResult(error = t.code.toString() + " : " + t.msg)
                }

                override fun onError(e: Throwable) {
                    Log.i(TAG, "onError: ${e.message}")
                }

                override fun onComplete() {
                }
            })
    }

    private fun loginForS(user : User) {
        RetrofitManager.getService(StudentAPI::class.java)
            .registerOrLoginForStu(user)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Results<Student>> {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext(t: Results<Student>) {
                    if (t.code == 200) {
                        if (t.data != null)
                            _loginResult.value = LoginResult(success = "true")
                        else _loginResult.value = LoginResult(success = "true", needRegister = true)
                    } else
                        _loginResult.value = LoginResult(error = t.code.toString() + " " + t.msg)
                }

                override fun onError(e: Throwable) {
                    Log.i(TAG, "onError: ${e.message}")
                }

                override fun onComplete() {
                }

            })
    }

    companion object {
        const val TAG = "LoginViewModel"
    }

}