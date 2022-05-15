package com.example.attendance.ui.arcface

import android.Manifest
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

    private var cameraHelper : CameraHelper? = null
    var faceHelper : FaceHelper? = null

    var previewSize : Camera.Size? = null
    var rgbCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT

    /**
     * VIDEO模式人脸检测引擎，用于预览帧人脸追踪
     */
    var ftEngine: FaceEngine? = null

    /**
     * 用于特征提取的引擎
     */
    var frEngine: FaceEngine? = null

    /**
     * IMAGE模式活体检测引擎，用于预览帧人脸活体检测
     */
    var flEngine: FaceEngine? = null

    var ftInitCode = -1
    var frInitCode = -1
    var flInitCode = -1


    /**
     * 用于记录人脸识别相关状态
     */
    val requestFeatureStatusMap = ConcurrentHashMap<Int, Int>()

    /**
     * 用于记录人脸特征提取出错重试次数
     */
    val extractErrorRetryMap = ConcurrentHashMap<Int, Int>()

    /**
     * 用于存储活体检测出错重试次数
     */
    val livenessErrorRetryMap = ConcurrentHashMap<Int, Int>()

    private val getFeatureDelayedDisposables: CompositeDisposable = CompositeDisposable()
    private val delayFaceTaskCompositeDisposable : CompositeDisposable = CompositeDisposable()


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
            initEngine()
            initCamera()
        }
    }

    private fun initView() {
        binding.singleCameraTexturePreview
            .viewTreeObserver.addOnGlobalLayoutListener(this)

    }

    private fun initEngine() {
        Log.d(TAG, "initEngine: ")
        ftEngine = FaceEngine()
        ftInitCode = ftEngine!!.init(
            this,
            DetectMode.ASF_DETECT_MODE_VIDEO,
            DetectFaceOrientPriority.valueOf(DetectFaceOrientPriority.ASF_OP_270_ONLY.name),
            16,
            MAX_DETECT_NUM,
            FaceEngine.ASF_FACE_DETECT
        )

        frEngine = FaceEngine()
        frInitCode = frEngine!!.init(
            this,
            DetectMode.ASF_DETECT_MODE_IMAGE,
            DetectFaceOrientPriority.ASF_OP_0_ONLY,
            16,
            MAX_DETECT_NUM,
            FaceEngine.ASF_FACE_RECOGNITION
        )

        flEngine = FaceEngine()
        flInitCode = flEngine!!.init(
            this,
            DetectMode.ASF_DETECT_MODE_IMAGE,
            DetectFaceOrientPriority.ASF_OP_0_ONLY,
            16,
            MAX_DETECT_NUM,
            FaceEngine.ASF_LIVENESS
        )

        Log.i(
            TAG,
            "initEngine:  init: $ftInitCode"
        )

        if (ftInitCode != ErrorInfo.MOK) {
            val error = getString(
                R.string.specific_engine_init_failed , "ftEngine", ftInitCode)
            Log.i(
                TAG,
                "initEngine: $error"
            )
        }
        if (frInitCode != ErrorInfo.MOK) {
            val error = getString(
                R.string.specific_engine_init_failed, "frEngine", frInitCode)
            Log.i(
                TAG,
                "initEngine: $error"
            )
        }
        if (flInitCode != ErrorInfo.MOK) {
            val error = getString(
                R.string.specific_engine_init_failed, "flEngine", flInitCode)
            Log.i(
                TAG,
                "initEngine: $error"
            )
        }
    }

    fun unInitEngine() {

    }

    private fun initCamera() {
        Log.d(TAG, "initCamera: ")
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        val faceListener = object : FaceListener {
            override fun onFail(e: Exception) {
                Log.e(TAG, "Face listener onFail: e.getMessage ")
            }

            override fun onFeatureInfoGet(
                faceFeature: FaceFeature?,
                requestId: Int,
                errorCode: Int
            ) {
                if (faceFeature != null)
                    searchFace(faceFeature, requestId)
                // 特征提取失败
                else {
                    if (increaseAndGetValue(extractErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        extractErrorRetryMap[requestId] = 0
                        val msg =
                            if (errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                                "face low confidence level"
                            } else
                                "ExtratCode : $errorCode"
                    } else
                        requestFeatureStatusMap[requestId] = 3
                }


            }

            override fun onFaceLivenessInfoGet(
                livenessInfo: LivenessInfo?,
                requestId: Int?,
                errorCode: Int?
            ) {
                TODO("Not yet implemented")
            }
        }

        val cameraListener = object : CameraListener {
            override fun onCameraOpened(
                camera: Camera,
                cameraId: Int,
                displayOrientation: Int,
                isMirror: Boolean
            ) {
                val lastPreviewSize = previewSize
                previewSize = camera.parameters.previewSize

                if (faceHelper == null ||
                    lastPreviewSize == null ||
                    lastPreviewSize.width != previewSize!!.width ||
                    lastPreviewSize.height != previewSize!!.height
                ) {
                    var trackedFaceCount : Int
                    faceHelper?.apply {
                        trackedFaceCount = getTrackedFaceCount()
                        release()
                    }
                    faceHelper = FaceHelper.Builder()
                        .ftEngine(ftEngine)
                        .frEngine(frEngine)
                        .flEngine(flEngine)
                        .frQueueSize(MAX_DETECT_NUM)
                        .flQueueSize(MAX_DETECT_NUM)
                        .previewSize(previewSize)
                        .faceListener(faceListener)
                        .build()
                }

            }

            override fun onPreview(data: ByteArray?, camera: Camera?) {
                faceHelper?.apply {
                    val facePreviewInfoList = onPreviewFrame(data)
                    if (facePreviewInfoList.isNotEmpty() && previewSize != null) {
                        for (i in facePreviewInfoList.indices) {
                            val status =
                                requestFeatureStatusMap[facePreviewInfoList[i].trackId]

                            /**
                             * 对于每个人脸，若状态为空或者为失败，则请求特征提取（可根据需要添加其他判断以限制特征提取次数），
                             * 特征提取回传的人脸特征结果在[FaceListener.onFaceFeatureInfoGet]中回传
                             */
                            if (status == null || status == 3) {
                                requestFeatureStatusMap[facePreviewInfoList[i].trackId!!] = 0
                                faceHelper!!.requestFaceFeature(
                                    data,
                                    facePreviewInfoList[i].faceInfo!!,
                                    previewSize!!.width,
                                    previewSize!!.height,
                                    FaceEngine.CP_PAF_NV21,
                                    facePreviewInfoList[i].trackId!!
                                )
                            }
                        }
                    }

                }
            }

            override fun onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ")
            }

            override fun onCameraError(e: Exception?) {
                Log.e(TAG, "onCameraError: ${e?.message}")
            }

            override fun onCameraConfigurationChanged(cameraID: Int, displayOrientation: Int) {

            }
        }

        val previewView = binding.singleCameraTexturePreview

        cameraHelper = CameraHelper.Builder()
            .previewViewSize(Point(previewView.measuredWidth, previewView.measuredHeight))
            .rotation(windowManager.defaultDisplay.rotation)
            .specificCameraId(rgbCameraId)
            .isMirror(false)
            .previewOn(previewView)
            .cameraListener(cameraListener)
            .build()
        cameraHelper?.apply {
            init()
            start()
        }
    }

    private fun searchFace(frFace : FaceFeature, requestId : Int) {
        Observable.create(ObservableOnSubscribe<CompareResult> {
            val compareResult = FaceServer.instance.getTopOfFaceLib(frFace)
            if (compareResult != null)
                it.onNext(compareResult)
            else it.onError(Exception("can not recognize face"))

        }).subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<CompareResult?> {
                override fun onSubscribe(d: Disposable) {}

                override fun onNext(t: CompareResult) {
                    if (t.username == null){
                        requestFeatureStatusMap[requestId] = 2

                    }

                    if (t.similar > 0.8f) {
                        val isAdded = false

                    }
                }

                override fun onError(e: Throwable) {
                    Log.d(TAG, "onError: ${e.message}")
                }

                override fun onComplete() {
                }

            })
    }

    /**
     * 将map中key对应的value增1回传
     *
     * @param countMap map
     * @param key      key
     * @return 增1后的value
     */
    fun increaseAndGetValue(
        countMap: ConcurrentHashMap<Int, Int>?,
        key: Int): Int
    {
        if (countMap == null) {
            return 0
        }
        var value = countMap[key]
        if (value == null) {
            value = 0
        }
        countMap[key] = ++value
        return value
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraHelper?.apply {
            release()
            cameraHelper = null
        }

        unInitEngine()

        getFeatureDelayedDisposables.apply { clear() }

        delayFaceTaskCompositeDisposable.apply { clear() }

        FaceServer.instance.unInit()

    }



    /**
     * 在第一次布局完成后，去除该监听，并且进行引擎和相机的初始化
     */

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

        private const val MAX_DETECT_NUM = 10

        /**
         *  当FR成功，活体未成功时，FR等待活体的时间
         */
        private const val WAIT_LIVENESS_INTERVAL = 100

        /**
         *  失败重试间隔 ms
         */
        private const val FAIL_RETRY_INTERVAL = 100

        /**
         * 失败重试次数
         */
        private const val MAX_RETRY_TIME = 3

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