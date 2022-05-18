package com.example.attendance

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.arcsoft.face.ErrorInfo
import com.arcsoft.face.FaceEngine
import com.arcsoft.face.VersionInfo
import com.example.attendance.common.Constants
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import kotlin.collections.ArrayList

class App() : Application() {


    private var libraryExists = true

    override fun onCreate() {
        super.onCreate()
        myApplication = this

    }


    companion object {
        const val TAG = "Attendance_app"

        private lateinit var myApplication: App

        fun getInstance(): App {
            return myApplication
        }
    }

}