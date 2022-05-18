package com.example.attendance.ui.report

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.attendance.api.StudentAPI
import com.example.attendance.api.TeacherAPI
import com.example.attendance.api.retrofit.Results
import com.example.attendance.api.retrofit.RetrofitManager
import com.example.attendance.model.AttendanceRecord
import com.example.attendance.model.CourseAttendanceRecord
import com.example.attendance.ui.attendance.AttendViewModel
import com.example.attendance.util.SharedPreferencesUtils
import com.example.attendance.util.ToastUtils
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class ReportViewModel : ViewModel() {

    private var _stuAttendRecords = MutableLiveData<List<AttendanceRecord>>()
    val stuAttendRecords : LiveData<List<AttendanceRecord>> = _stuAttendRecords

    private var _courseRecords = MutableLiveData<List<CourseAttendanceRecord>>()
    val courseRecords : LiveData<List<CourseAttendanceRecord>>  = _courseRecords

    init {
        when(SharedPreferencesUtils.getCurrentUserRole()) {
            0 -> getRecordForT()
            1 -> getRecordForS()
            else -> ToastUtils.showShortToast("undefined role")
        }
    }

    private fun getRecordForT() {
       RetrofitManager.getService(TeacherAPI::class.java)
           .getRecordForT()
           .subscribeOn(Schedulers.io())
           .observeOn(AndroidSchedulers.mainThread())
           .subscribe(object : Observer<Results<List<CourseAttendanceRecord>>> {
               override fun onSubscribe(d: Disposable) {}

               override fun onNext(t: Results<List<CourseAttendanceRecord>>) {
                    if (t.code == 200) {
                        t.data?.apply {
                            _courseRecords.value = this
                        }
                    } else Log.e(TAG, "onNext: code : ${t.code}, msg : ${t.msg}" )

               }

               override fun onError(e: Throwable) {
                   Log.e(TAG, "onError: ${e.message}", )
               }

               override fun onComplete() {
                   TODO("Not yet implemented")
               }

           })
    }

    private fun getRecordForS() {
        RetrofitManager.getService(StudentAPI::class.java)
            .getRecordForS()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Results<List<AttendanceRecord>>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onNext(t: Results<List<AttendanceRecord>>) {
                    if (t.code == 200) {
                        t.data?.apply {
                            _stuAttendRecords.value = this
                        }
                    } else Log.e(TAG, "onNext: code : ${t.code}, msg : ${t.msg}" )

                }

                override fun onError(e: Throwable) {
                    Log.e(TAG, "onError: ${e.message}")
                }
                override fun onComplete() {}
            })

    }
    companion object {
        const val TAG = "ReportViewModel"
    }


}