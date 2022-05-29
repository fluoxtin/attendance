package com.example.attendance.ui.attendance

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.example.attendance.App
import com.example.attendance.api.StudentAPI
import com.example.attendance.api.TeacherAPI
import com.example.attendance.api.retrofit.Results
import com.example.attendance.api.retrofit.RetrofitManager
import com.example.attendance.faceserver.FaceServer
import com.example.attendance.model.*
import com.example.attendance.oss.OSSUploader
import com.example.attendance.util.Distance
import com.example.attendance.util.SharedPreferencesUtils
import com.example.attendance.util.ToastUtils
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

class AttendViewModel : ViewModel() {

    private val _currentStudent = MutableLiveData<Student>()
    val currentStudent : LiveData<Student> = _currentStudent

    private val _currentTeacher = MutableLiveData<Teacher>()
    val currentTeacher : LiveData<Teacher> = _currentTeacher

    private val _courses = MutableLiveData<List<Course>>()
    val course : LiveData<List<Course>> = _courses

    private val _attendTask = MutableLiveData<AttendTask?>()
    val attendTask : LiveData<AttendTask?> = _attendTask

    private val _currLocation = MutableLiveData<BDLocation>()
    val currLocation : LiveData<BDLocation> = _currLocation

    private val _destinationLocation = MutableLiveData<Location>()
    val destinationLocation : LiveData<Location> = _destinationLocation

    private val _canSignIn = MutableLiveData<Boolean>()
    val canSignIn : LiveData<Boolean> = _canSignIn

    private val _task = MutableLiveData<AttendTask?>()
    val task : LiveData<AttendTask?> = _task

    private val _countdown = MutableLiveData<Int?>()
    val countdown : LiveData<Int?> = _countdown

    private val BD_LISTENER = object : BDAbstractLocationListener() {
        override fun onReceiveLocation(location: BDLocation?) {
            location?.apply {
                _currLocation.value = this
                curLocation = Location(
                    latitude,
                    longitude
                )
                Log.d(TAG, "onReceiveLocation: $curLocation")
                val addr = location.addrStr
                val country = location.country
                val province = location.province

                Log.d(TAG, "addr : $addr, country : $country, rovince : $province")

            }
        }
    }

    var locationClient : LocationClient? = null
    var curLocation : Location? = null

    var compositeDisposable = CompositeDisposable()
    var disposable : Disposable? = null

    init {
        SharedPreferencesUtils.getCurrentUser()?.apply {
            if (role == 0) {
                getTeacherInfo()
                getTeacherCourse()
                getCurTask()
            } else if (role == 1) {
                getStudentInfo()
                getStudentCourse()
                start()
                getTaskForS()
            }
        }
    }

    fun getTeacherInfo() {
        RetrofitManager.getService(TeacherAPI::class.java)
            .getTeacherInfo()
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

    private fun getStudentInfo() {
        RetrofitManager.getService(StudentAPI::class.java)
            .getStudentInfo()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Results<Student>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onNext(t: Results<Student>) {
                    t.apply {
                        if (code == 200) {
                            data?.apply {
                                Log.d(TAG, "getStudentInfo : $data")
                                _currentStudent.value = this
                            }
                        } else {
                            Log.e(TAG, "onNext: error code : ${t.code} & msg : ${t.msg}")
                        }
                    }

                }

                override fun onError(e: Throwable) {
                    Log.e(TAG, "getStudentInfo onError: ${e.message}")
                }

                override fun onComplete() {}
            })
    }

    fun loadFace(key : String) {
        Log.d(TAG, "loadFace: ")
        val isExists = fileIsExists(App.getInstance().filesDir.absolutePath + File.separator +
        FaceServer.SAVE_IMG_DIR + File.separator + key + FaceServer.IMG_SUFFIX)
        if (isExists)
            return
        else OSSUploader.instance.downloadFile(key)
    }

    private fun fileIsExists(path : String) : Boolean {
        try {
            val f = File(path)
            if (!f.exists())
                return false
        } catch (e : Exception) {
            return false
        }
        return true
    }


    private fun getStudentCourse() {
        RetrofitManager.getService(StudentAPI::class.java)
            .getCourses()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
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
                    Log.e(TAG, "getStudentCourse onError: ${e.message}")
                }

                override fun onComplete() {}
            })
    }

    private fun getTeacherCourse() {
        RetrofitManager.getService(TeacherAPI::class.java)
            .getCourses()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
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

    fun postTask(course: Course) {

        val location = Location(
            currLocation.value?.latitude ?: 0.0,
            currLocation.value?.longitude ?: 0.0
        )


        val task = AttendTask(
            UUID.randomUUID().toString(),
            course.cour_id,
            location,
            System.currentTimeMillis() + LIMITED_TIME
        )

        RetrofitManager.getService(TeacherAPI::class.java)
            .postTask(task)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Results<Any>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onNext(t: Results<Any>) {
                    if (t.code == 200){
                        ToastUtils.showShortToast("发布任务成功")
                        Log.i(TAG, "onNext: post task success!")
                        getCurTask()
                    }

                }

                override fun onError(e: Throwable) {
                    Log.e(TAG, "onError: ${e.message}" )
                }

                override fun onComplete() {}
            })
    }

    private fun getCurTask() {
        RetrofitManager.getService(TeacherAPI::class.java)
            .getCurTask()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Results<AttendTask>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onNext(t: Results<AttendTask>) {
                    t.apply {
                        if (code == 200) {
                            data?.apply {
                                _task.value = this
                            }
                        }
                    }
                }

                override fun onError(e: Throwable) {
                    ToastUtils.showShortToast("error : ${e.message}")
                    Log.d(TAG, "getCurTask: ${e.message} & ${e.cause}")
                }

                override fun onComplete() {}
            })
    }

    private fun getTaskForS() {
        compositeDisposable.add(
            Observable.interval(10,120, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    RetrofitManager.getService(StudentAPI::class.java)
                        .getTask()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : Observer<Results<AttendTask>> {
                            override fun onSubscribe(d: Disposable) {}

                            override fun onNext(t: Results<AttendTask>) {
                                if (t.code == 200)
                                    t.data?.apply {
                                        _attendTask.value = this
                                    }
                                else Log.e(TAG, "onNext: code : ${t.code}, msg : ${t.msg}" )
                            }

                            override fun onError(e: Throwable) {
                                Log.e(TAG, "getTask onError: ${e.message}")
                            }

                            override fun onComplete() {}

                        })
                }
        )
    }

    fun startCountdown(deadline : Long) {
        val time = (deadline - System.currentTimeMillis()) / 1000
        if (time < 0) {
            ToastUtils.showLongToast("countdown time < 0, task is overdue")
            return
        }
        disposable?.apply {
            dispose()
        }
        disposable = Flowable.intervalRange(0,
            time,
            0,
            1, TimeUnit.SECONDS
        ).observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                _countdown.value = (time - it).toInt()
            }
            .doOnComplete { _task.value = null
                _countdown.value = null
            }.subscribe()
    }

    fun stopCountdown() {
        disposable?.apply {
            dispose()
        }
    }

    fun canSignIn(location : Location) {

        val p2 = Location(curLocation?.latitude ?: 0.0, curLocation?.longitude ?: 0.0)
        val distance = Distance.getDistance(location, p2)
        Log.d(TAG, "canSignIn: distance : $distance")
        _canSignIn.value = distance < 50
    }

    fun postAttendanceRecord(task : AttendTask, attend : Int) {
        val record = AttendanceRecord(
            task.attend_id,
            task.cour_id,
            System.currentTimeMillis(),
            attend
        )

        RetrofitManager.getService(StudentAPI::class.java)
            .postRecord(record)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Results<AttendanceRecord>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onNext(t: Results<AttendanceRecord>) {
                    t.apply {
                        if (code == 200) {
                            ToastUtils.showShortToast("post record successfully")
                            _attendTask.value = null
                            stopCountdown()
                        } else{
                            ToastUtils.showLongToast("Post record failed")
                            Log.d(TAG, "onNext: post record failed ${t.code}")
                        }
                    }
                }

                override fun onError(e: Throwable) {
                    e.message?.let { ToastUtils.showShortToast(it) }
                    Log.d(TAG, "onError: ${e.message}")
                }

                override fun onComplete() {}
            })

    }

     fun updateFaceUrl(key : String) {
         val student = _currentStudent.value!!
         student.face_url = key
         RetrofitManager.getService(StudentAPI::class.java)
             .updateFace(student)
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
                             Log.d(TAG, "error code : $code,$msg")
                         }
                     }
                 }

                 override fun onError(e: Throwable) {
                     Log.d(TAG, "update face onError: $e")
                 }

                 override fun onComplete() {
                 }

             })
     }

    fun start() {
        compositeDisposable.dispose()
        compositeDisposable = CompositeDisposable()
    }

    fun stop() {
        compositeDisposable.apply {
            this.dispose()
        }
        if (locationClient != null) {
            locationClient?.stop()
            locationClient!!.unRegisterLocationListener(BD_LISTENER)
            locationClient = null
        }
    }

    fun initLocationClient(context: Context) {
        locationClient = LocationClient(context)
        locationClient?.registerLocationListener(BD_LISTENER)
        val option = LocationClientOption()
        option.setIsNeedAddress(true)
        option.setNeedNewVersionRgc(true)
        locationClient?.locOption = option
        locationClient?.start()
    }


    companion object {
        const val TAG = "ViewModel"
        const val LIMITED_TIME = 5 * 60 * 1000

    }

}