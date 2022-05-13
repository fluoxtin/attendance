package com.example.attendance.ui.attendance

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.attendance.api.StudentAPI
import com.example.attendance.api.TeacherAPI
import com.example.attendance.api.retrofit.Results
import com.example.attendance.api.retrofit.RetrofitManager
import com.example.attendance.model.*
import com.example.attendance.util.SharedPreferencesUtils
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class AttendViewModel : ViewModel() {

    private val _currentUser = MutableLiveData<User>()
    val currentUser : LiveData<User> = _currentUser

    private val _currentStudent = MutableLiveData<Student>()
    val currentStudent : LiveData<Student> = _currentStudent

    private val _currentTeacher = MutableLiveData<Teacher>()
    val currentTeacher : LiveData<Teacher> = _currentTeacher

    private val _courses = MutableLiveData<List<Course>>()
    val course : LiveData<List<Course>> = _courses

    private val _attendTask = MutableLiveData<AttendTask>()
    val attendTask : LiveData<AttendTask> = _attendTask

    var compositeDisposable = CompositeDisposable()

    init {
        SharedPreferencesUtils.getCurrentUser()?.apply {
            _currentUser.value = this
        }
    }

    fun getTeacherInfo(tea_id : String) {
        RetrofitManager.getService(TeacherAPI::class.java)
            .getTeacherInfo(tea_id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Results<Teacher>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onNext(t: Results<Teacher>) {
                    t.apply {
                        if (code == 200) {
                            data?.apply { _currentTeacher.value = this }
                        } else {
                            Log.e(TAG, "onNext: error code : $code  & msg = $msg" )
                        }
                    }
                }

                override fun onError(e: Throwable) {
                    Log.e(TAG, "onError: ${e.message}")
                }

                override fun onComplete() {}
            })
    }

    fun getStudentInfo(stu_id : String) {
        RetrofitManager.getService(StudentAPI::class.java)
            .getStudentInfo(stu_id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Results<Student>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onNext(t: Results<Student>) {
                    t.apply {
                        if (code == 200) {
                           data?.apply {
                               _currentStudent.value = this
                           }
                        } else {
                            Log.e(TAG, "onNext: error code : ${t.code} & msg : ${t.msg}" )
                        }
                    }

                }

                override fun onError(e: Throwable) {
                    Log.e(TAG, "onError: ${e.message}")
                }

                override fun onComplete() {}
            })
    }

    fun getStudentCourse(id : String) {
        RetrofitManager.getService(StudentAPI::class.java)
            .getCourses(id)
            .observeOn(Schedulers.io())
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Results<List<Course>>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onNext(t: Results<List<Course>>) {
                    if (t.code == 200) {
                        t.data?.apply {
                            _courses.value = this
                        }
                    } else {
                        Log.e(TAG, "onNext: ${t.code}, msg : ${t.msg}" )
                    }
                }

                override fun onError(e: Throwable) {
                    Log.e(TAG, "onError: ${e.message}")
                }

                override fun onComplete() {}
            })
    }

    fun getTeacherCourse(id : String) {
        RetrofitManager.getService(TeacherAPI::class.java)
            .getCourses(id)
            .observeOn(Schedulers.io())
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Results<List<Course>>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onNext(t: Results<List<Course>>) {
                    if (t.code == 200) {
                        t.data?.apply { _courses.value = this }
                    } else
                        Log.e(TAG, "onNext: code : ${t.code}, msg : ${t.msg}")
                }

                override fun onError(e: Throwable) {
                    Log.e(TAG, "onError: ${e.message}" )
                }

                override fun onComplete() {}
            })
    }

    fun postTask(task : AttendTask) {
        RetrofitManager.getService(TeacherAPI::class.java)
            .postTask(task)
            .observeOn(Schedulers.io())
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Results<Any>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onNext(t: Results<Any>) {
                    if (t.code == 200)
                        Log.i(TAG, "onNext: post task success!")

                }

                override fun onError(e: Throwable) {
                    Log.e(TAG, "onError: ${e.message}" )
                }

                override fun onComplete() {}
            })
    }

    fun getTaskForS(id : String) {
        compositeDisposable.add(
            Observable.interval(3, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    RetrofitManager.getService(StudentAPI::class.java).getTask(id)
                        .observeOn(Schedulers.io())
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : Observer<Results<AttendTask>> {
                            override fun onSubscribe(d: Disposable) {}

                            override fun onNext(t: Results<AttendTask>) {
                                if (t.code == 200)
                                    t.data?.apply { _attendTask.value = this }
                                else Log.e(TAG, "onNext: code : ${t.code}, msg : ${t.msg}" )
                            }

                            override fun onError(e: Throwable) {
                                Log.e(TAG, "onError: ${e.message}")
                            }

                            override fun onComplete() {}

                        })
                }
        )
    }

    fun start() {
        compositeDisposable.dispose()
        compositeDisposable = CompositeDisposable()
    }

    fun stop() {
        compositeDisposable?.apply {
            this.dispose()
        }
    }



    companion object {
        const val TAG = "ViewModel"
    }

}