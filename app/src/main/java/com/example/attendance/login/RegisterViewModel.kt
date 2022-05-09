package com.example.attendance.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.attendance.R
import com.example.attendance.model.Student
import com.example.attendance.model.Teacher
import com.example.attendance.retrofit.Results
import com.example.attendance.retrofit.RetrofitManager
import com.example.attendance.util.SharedPreferencesUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterViewModel() : ViewModel() {

    private val _registerFormState = MutableLiveData<RegisterFormState>()
    val registerFormState : LiveData<RegisterFormState> = _registerFormState

    private val _registerResult = MutableLiveData<RegisterResult>()
    val registerResult : LiveData<RegisterResult> = _registerResult

    fun nameDataChanged(name : String) {
        if (name.isBlank())
            _registerFormState.value = RegisterFormState(nameError = R.string.name_error)
        else
            _registerFormState.value = RegisterFormState(nameError = null)
    }

    fun phoneDataChanged(phone : String) {
        _registerFormState.value = when {
            phone.isBlank() -> RegisterFormState(phoneError = R.string.phone_error)
            else -> RegisterFormState(phoneError = null)
        }
    }

    fun emailDataChanged(email : String) {
        _registerFormState.value = when {
            email.isBlank() -> RegisterFormState(emailError = R.string.email_error)
            else -> RegisterFormState(emailError = null)
        }
    }

    fun unitDataChanged(unit : String) {
        _registerFormState.value = when {
            unit.isBlank() -> RegisterFormState(unitError = R.string.unit_error)
            else -> RegisterFormState(unitError = null)
        }
    }

    fun classDataChanged(stuClass : String) {
        _registerFormState.value = when {
            stuClass.isBlank() -> RegisterFormState(stuClassError = R.string.class_error)
            else -> RegisterFormState(stuClassError = null)
        }
    }

    fun majorDataChanged(major : String) {
        _registerFormState.value = when {
            major.isBlank() -> RegisterFormState(majorError = R.string.major_error)
            else -> RegisterFormState(majorError = null)
        }
    }

    fun updateStudentInfo(student: Student) {
        val apiService = RetrofitManager.getService(APIService::class.java)
        apiService.updateStudentInfo(SharedPreferencesUtils.getToken(), student)
            .enqueue(object : Callback<Results<Student>>{
                override fun onResponse(
                    call: Call<Results<Student>>,
                    response: Response<Results<Student>>
                ) {
                    response.body()?.apply {
                        if (success != null) {
                            _registerResult.value = RegisterResult(success = message)
                        } else
                            _registerResult.value = RegisterResult(error = response.code())
                    }
                }

                override fun onFailure(call: Call<Results<Student>>, t: Throwable) {
                    Log.d(TAG, "onFailure: ${t.message}")
                }
        })
    }

    fun updateTeaInfo(teacher: Teacher) {
        val apiService = RetrofitManager.getService(APIService::class.java)
            apiService.updateTeacherInfo(SharedPreferencesUtils.getToken(), teacher)
                .enqueue(object : Callback<Results<Teacher>> {
                    override fun onResponse(
                        call: Call<Results<Teacher>>,
                        response: Response<Results<Teacher>>
                    ) {
                        response.body()?.apply {
                            if (success != null) {
                                _registerResult.value = RegisterResult(success = message)
                            } else
                                _registerResult.value = RegisterResult(error = response.code())
                        }
                    }

                    override fun onFailure(call: Call<Results<Teacher>>, t: Throwable) {
                        Log.d(TAG, "onFailure: ${t.message}")
                    }

                })
    }

    companion object {
        const val TAG = "RegisterViewModel"
    }

}