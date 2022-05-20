package com.example.attendance.ui.report

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendance.api.TeacherAPI
import com.example.attendance.api.retrofit.Results
import com.example.attendance.api.retrofit.RetrofitManager
import com.example.attendance.databinding.ActivityRecordDetailBinding
import com.example.attendance.model.CourseAttendanceRecord
import com.example.attendance.model.StudentRecord
import com.example.attendance.util.ToastUtils
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class RecordDetailActivity : AppCompatActivity() {

    lateinit var binding: ActivityRecordDetailBinding
    lateinit var adapter: StudentRecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = StudentRecordAdapter()
        binding.stuRecord.adapter = adapter
        binding.stuRecord.layoutManager = LinearLayoutManager(this)

        val record = intent.getSerializableExtra("record") as CourseAttendanceRecord

        getAllRecord(record)

    }

    private fun getAllRecord(record: CourseAttendanceRecord) {
        RetrofitManager.getService(TeacherAPI::class.java)
            .getStuRecords(record)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Results<List<StudentRecord>>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onNext(t: Results<List<StudentRecord>>) {
                    t.apply {
                        if (code == 200){
                            data?.apply {
                                adapter.submitList(this)
                            }
                            Log.d(ReportViewModel.TAG, "get all record $msg")
                        } else {
                            ToastUtils.showLongToast("$code $msg")
                        }
                    }
                }

                override fun onError(e: Throwable) {
                    Log.d(ReportViewModel.TAG, "getAllRecord onError : ${e.message} ${e.cause}")
                }

                override fun onComplete() {}
            })
    }

}