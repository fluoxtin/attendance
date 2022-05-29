package com.example.attendance.ui.arcface

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Point
import android.hardware.Camera
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arcsoft.face.ErrorInfo
import com.arcsoft.face.FaceEngine
import com.arcsoft.face.FaceFeature
import com.arcsoft.face.LivenessInfo
import com.arcsoft.face.enums.DetectFaceOrientPriority
import com.arcsoft.face.enums.DetectMode
import com.example.attendance.R
import com.example.attendance.camera.CameraHelper
import com.example.attendance.camera.CameraListener
import com.example.attendance.databinding.ActivityRecognizeFaceBinding
import com.example.attendance.faceserver.CompareResult
import com.example.attendance.faceserver.FaceHelper
import com.example.attendance.faceserver.FaceListener
import com.example.attendance.faceserver.FaceServer
import com.example.attendance.util.SharedPreferencesUtils
import com.example.attendance.util.ToastUtils
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.ConcurrentHashMap

class RecognizeFaceActivity : AppCompatActivity(), ViewTreeObserver.OnGlobalLayoutListener {

    lateinit var binding : ActivityRecognizeFaceBinding

    private val faceViewModel by viewModels<FaceViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecognizeFaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val attributes = window.attributes
        attributes.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        window.attributes = attributes

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

        Log.d(TAG, "onCreate: ")
        initView()
    }

    /**
     * 在第一次布局完成后，去除该监听，并且进行引擎和相机的初始化
     */
    override fun onGlobalLayout() {
        binding.singleCameraTexturePreview
            .viewTreeObserver.removeOnGlobalLayoutListener(this)
        if (!checkPermissions(NEEDED_PERMISSIONS))
            ActivityCompat.requestPermissions(
                this,
                NEEDED_PERMISSIONS,
                ACTION_REQUEST_PERMISSIONS
            )
        else {
            faceViewModel.initEngine(this)
            faceViewModel.initCamera(
                binding.singleCameraTexturePreview,
                windowManager.defaultDisplay.rotation)
        }
    }

    private fun initView() {
        binding.singleCameraTexturePreview
            .viewTreeObserver.addOnGlobalLayoutListener(this)

        faceViewModel.recognizeResult.observe(this) {
            it?.apply {
                val user = SharedPreferencesUtils.getCurrentUser()
                val intent = Intent()
                when {
                    username != user!!.username -> {
                        intent.putExtra("recognized", false)
                    }
                    similar < 0.8 -> intent.putExtra("recognized", false)
                    else -> {
                        val alertDialog = AlertDialog.Builder(this@RecognizeFaceActivity)
                            .setTitle("人脸识别成功！")
                            .setMessage("你当前已完成人脸识别，是否需要立即完成签到？")
                            .setNegativeButton("否") { dialog, _ ->
                                dialog.cancel()
                                setResult(RESULT_OK, intent)
                                finish()
                            }
                            .setPositiveButton("是") { dialog, _ ->
                                intent.putExtra("recognized", true)
                                dialog.dismiss()
                                setResult(RESULT_OK)
                                finish()
                            }
                            .create()
                        alertDialog.setCancelable(false)
                        alertDialog.show()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        faceViewModel.stop()
    }

    private fun checkPermissions(neededPermissions: Array<String>): Boolean {
        if (neededPermissions.isEmpty()) {
            return true
        }
        var allGranted = true
        for (neededPermission in neededPermissions) {
            allGranted = allGranted and (ContextCompat.checkSelfPermission(
                this,
                neededPermission
            ) == PackageManager.PERMISSION_GRANTED)
        }
        return allGranted
    }


    companion object {
        const val TAG = "RecognizeFaceActivity"

        private const val ACTION_REQUEST_PERMISSIONS = 0x001

        /**
         * 所需的所有权限信息
         */
        private val NEEDED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE
        )
    }
}