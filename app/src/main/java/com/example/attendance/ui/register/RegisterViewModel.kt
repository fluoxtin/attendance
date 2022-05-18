package com.example.attendance.ui.register

import android.util.Log
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.attendance.R
import com.example.attendance.api.StudentAPI
import com.example.attendance.api.TeacherAPI
import com.example.attendance.model.Student
import com.example.attendance.model.Teacher
import com.example.attendance.api.retrofit.Results
import com.example.attendance.api.retrofit.RetrofitManager
import com.example.attendance.util.SharedPreferencesUtils
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class RegisterViewModel : ViewModel() {

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
            unit.isBlank() -> RegisterFormState(unitError = R.string.tea_unit_error)
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
        val apiService = RetrofitManager.getService(StudentAPI::class.java)
        apiService.updateStudentInfo(student)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Results<Student>> {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext(t: Results<Student>) {
                    t.apply {
                        if (code == 200)
                            _registerResult.value = RegisterResult(success = msg)
                        else _registerResult.value = RegisterResult(error = code)
                    }
                }

                override fun onError(e: Throwable) {
                    Log.e(TAG, "onError: ${e.message}", )
                }

                override fun onComplete() {
                }

            })
    }

    fun updateTeaInfo(teacher: Teacher) {
//        val apiService = RetrofitManager.getService(TeacherAPI::class.java)
//            apiService.updateTeacherInfo(teacher)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe({ t ->
//                    t?.apply {
//                        if (code == 200)
//                            _registerResult.value = RegisterResult(success = msg)
//                        else _registerResult.value = RegisterResult(error = code)
//                    }
//                }) { t -> Log.e(TAG, "accept: $t?.message")
//                }.dispose()
        RetrofitManager.getService(TeacherAPI::class.java)
            .updateTeacherInfo(teacher)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Results<Teacher>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onNext(t: Results<Teacher>) {
                    if (t.code == 200) {
                        _registerResult.value = RegisterResult(success = t.msg)
                    } else
                        _registerResult.value = RegisterResult(error = t.code, errorMsg = t.msg)
                }

                override fun onError(e: Throwable) {
                    _registerResult.value = RegisterResult(errorMsg = e.message)
                }

                override fun onComplete() {
                }

            })
    }

    companion object {
        const val TAG = "RegisterViewModel"
    }

}