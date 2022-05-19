package com.example.attendance.ui.arcface

import android.content.Context
import android.graphics.Point
import android.hardware.Camera
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arcsoft.face.ErrorInfo
import com.arcsoft.face.FaceEngine
import com.arcsoft.face.FaceFeature
import com.arcsoft.face.LivenessInfo
import com.arcsoft.face.enums.DetectFaceOrientPriority
import com.arcsoft.face.enums.DetectMode
import com.example.attendance.R
import com.example.attendance.camera.CameraHelper
import com.example.attendance.camera.CameraListener
import com.example.attendance.faceserver.*
import com.example.attendance.util.ToastUtils
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class FaceViewModel() : ViewModel() {

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
     * 注册人脸状态码，准备注册
     */
    private val REGISTER_STATUS_READY = 0

    /**
     * 注册人脸状态码，注册中
     */
    private val REGISTER_STATUS_PROCESSING = 1

    /**
     * 注册人脸状态码，注册结束（无论成功失败）
     */
    private val REGISTER_STATUS_DONE = 2

    private var registerStatus: Int = REGISTER_STATUS_DONE

    /**
     * 用于记录人脸识别相关状态
     */
    val requestFeatureStatusMap = ConcurrentHashMap<Int, Int>()

    /**
     * 用于记录人脸特征提取出错重试次数
     */
    val extractErrorRetryMap = ConcurrentHashMap<Int, Int>()

    /**
     * 用于存储活体值
     */
    private val livenessMap = ConcurrentHashMap<Int, Int>()

    /**
     * 用于存储活体检测出错重试次数
     */
    private val livenessErrorRetryMap = ConcurrentHashMap<Int, Int>()


    private val getFeatureDelayedDisposables: CompositeDisposable = CompositeDisposable()
    private val delayFaceTaskCompositeDisposable : CompositeDisposable = CompositeDisposable()

    private val _recognizeResult = MutableLiveData<CompareResult>()
    val recognizeResult : LiveData<CompareResult> = _recognizeResult

    fun initEngine(context : Context) {
        Log.d(RecognizeFaceActivity.TAG, "initEngine: ")
        ftEngine = FaceEngine()
        ftInitCode = ftEngine!!.init(
            context,
            DetectMode.ASF_DETECT_MODE_VIDEO,
            DetectFaceOrientPriority.valueOf(DetectFaceOrientPriority.ASF_OP_270_ONLY.name),
            16,
            MAX_DETECT_NUM,
            FaceEngine.ASF_FACE_DETECT
        )

        frEngine = FaceEngine()
        frInitCode = frEngine!!.init(
            context,
            DetectMode.ASF_DETECT_MODE_IMAGE,
            DetectFaceOrientPriority.ASF_OP_0_ONLY,
            16,
            MAX_DETECT_NUM,
            FaceEngine.ASF_FACE_RECOGNITION
        )

        flEngine = FaceEngine()
        flInitCode = flEngine!!.init(
            context,
            DetectMode.ASF_DETECT_MODE_IMAGE,
            DetectFaceOrientPriority.ASF_OP_0_ONLY,
            16,
            MAX_DETECT_NUM,
            FaceEngine.ASF_LIVENESS
        )

        Log.i(
            RecognizeFaceActivity.TAG,
            "initEngine:  init: $ftInitCode"
        )

        if (ftInitCode != ErrorInfo.MOK) {
            val error = context.getString(
                R.string.specific_engine_init_failed ,
                "ftEngine",
                ftInitCode
            )
            Log.i(
                RecognizeFaceActivity.TAG,
                "initEngine: $error"
            )
        }
        if (frInitCode != ErrorInfo.MOK) {
            val error = context.getString(
                R.string.specific_engine_init_failed, "frEngine", frInitCode)
            Log.i(
                RecognizeFaceActivity.TAG,
                "initEngine: $error"
            )
        }
        if (flInitCode != ErrorInfo.MOK) {
            val error = context.getString(
                R.string.specific_engine_init_failed, "flEngine", flInitCode)
            Log.i(
                RecognizeFaceActivity.TAG,
                "initEngine: $error"
            )
        }
    }

    fun unInitEngine() {
        if (ftInitCode == ErrorInfo.MOK && ftEngine != null) {
            synchronized(ftEngine!!){
                val ftUnInitCode = ftEngine!!.unInit()
                Log.d(TAG, "ftEngine unInitEngine: $ftUnInitCode")
            }
        }
        if (frInitCode == ErrorInfo.MOK && frEngine != null) {
            synchronized(frEngine!!) {
                val frUnInitCode = frEngine!!.unInit()
                Log.d(TAG, "frEngine unInitEngine: $frUnInitCode")
            }
        }
        flEngine?.apply {
            if (flInitCode == ErrorInfo.MOK) {
                synchronized(this) {
                    val code = this.unInit()
                    Log.d(TAG, "flEngine unInitEngine: $code")
                }
            }
        }
    }

    fun initCamera(previewView : View, rotation : Int) {
        Log.d(RecognizeFaceActivity.TAG, "initCamera: ")

        val faceListener = object : FaceListener {
            override fun onFail(e: Exception) {
                Log.e(RecognizeFaceActivity.TAG, "Face listener onFail: e.getMessage ")
            }

            override fun onFeatureInfoGet(
                faceFeature: FaceFeature?,
                requestId: Int,
                errorCode: Int
            ) {
                if (faceFeature != null) {
                    val liveness = livenessMap[requestId]
                    if (liveness != null && liveness == LivenessInfo.ALIVE) {
                        searchFace(faceFeature, requestId)
                    } else {
                        //活体检测未出结果，或者非活体，延迟执行该函数
                        if (requestFeatureStatusMap.contains(requestId)) {
                            Observable.timer(WAIT_LIVENESS_INTERVAL, TimeUnit.MILLISECONDS)
                                .subscribe(object : Observer<Long> {
                                    lateinit var disposable: Disposable
                                    override fun onSubscribe(d: Disposable) {
                                        disposable = d
                                        getFeatureDelayedDisposables.add(disposable)
                                    }

                                    override fun onNext(t: Long) {
                                        onFeatureInfoGet(faceFeature, requestId, errorCode)
                                    }

                                    override fun onError(e: Throwable) {
                                    }

                                    override fun onComplete() {
                                        getFeatureDelayedDisposables.remove(disposable)
                                    }

                                })
                        }
                    }
                } else {
                    // 特征提取失败
                    if (increaseAndGetValue(extractErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        extractErrorRetryMap[requestId] = 0
                        val msg = if (errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL)
                            "人脸置信度低"
                        else
                            "ExtractCode : $errorCode"
//                        faceHelper?.setName(requestId, "未通过: $msg")
                        requestFeatureStatusMap[requestId] = 2
                        retryRecognizeDelayed(requestId)
                    } else {
                        requestFeatureStatusMap[requestId] = 3
                    }
                }


            }

            override fun onFaceLivenessInfoGet(
                livenessInfo: LivenessInfo?,
                requestId: Int,
                errorCode: Int?
            ) {
                if (livenessInfo != null) {
                    val liveness = livenessInfo.liveness
                    livenessMap[requestId] = liveness
                    if (liveness == LivenessInfo.NOT_ALIVE) {
//                        faceHelper?.setName(requestId, "未通过:  NOT_ALICE")
                        retryLivenessDetectDelayed(requestId)
                    }
                } else {
                    if (increaseAndGetValue(livenessErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        livenessErrorRetryMap[requestId] = 0
                        val msg = if (errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL)
                            "人脸置信度低"
                        else
                            "processCode : $errorCode"
//                        faceHelper?.setName(requestId, "未通过: $msg")
                        retryLivenessDetectDelayed(requestId)
                    } else
                        livenessMap[requestId] = -1
                }
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
                    if (data != null)
                        registerFace(data, facePreviewInfoList)

                    if (facePreviewInfoList.isNotEmpty() && previewSize != null) {
                        for (i in facePreviewInfoList.indices) {
                            val status =
                                requestFeatureStatusMap[facePreviewInfoList[i].trackId]

                            /**
                             * 在活体检测开启，在人脸识别状态不为成功或人脸活体状态不为处理中（ANALYZING）
                             * 且不为处理完成（ALIVE、NOT_ALIVE）时重新进行活体检测
                             */

                            if (status == null || status != 1) {
                                val liveness = livenessMap[facePreviewInfoList[i].trackId]
                                if (liveness == null || (liveness != LivenessInfo.ALIVE &&
                                            liveness != LivenessInfo.NOT_ALIVE &&
                                            liveness != 10)) {
                                    livenessMap[facePreviewInfoList[i].trackId!!] = 10
                                    requestFaceLiveness(
                                        data,
                                        facePreviewInfoList[i].faceInfo,
                                        previewSize!!.width,
                                        previewSize!!.height,
                                        FaceEngine.CP_PAF_NV21,
                                        facePreviewInfoList[i].trackId!!
                                    )
                                }
                            }

                            /**
                             * 对于每个人脸，若状态为空或者为失败，则请求特征提取（可根据需要添加其他判断以限制特征提取次数），
                             * 特征提取回传的人脸特征结果在[FaceListener.onFeatureInfoGet]中回传
                             */
                            if (status == null || status == 3) {
                                requestFeatureStatusMap[facePreviewInfoList[i].trackId!!] = 0
                                requestFaceFeature(
                                    data,
                                    facePreviewInfoList[i].faceInfo,
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
                Log.i(RecognizeFaceActivity.TAG, "onCameraClosed: ")
            }

            override fun onCameraError(e: Exception?) {
                Log.e(RecognizeFaceActivity.TAG, "onCameraError: ${e?.message}")
            }

            override fun onCameraConfigurationChanged(cameraID: Int, displayOrientation: Int) {

            }
        }


        cameraHelper = CameraHelper.Builder()
            .previewViewSize(Point(previewView.measuredWidth, previewView.measuredHeight))
            .rotation(rotation)
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

    /**
     *  register face
     */
    fun register() {
        if (registerStatus == REGISTER_STATUS_DONE) {
            registerStatus = REGISTER_STATUS_READY
        }
    }

    fun registerFace(nv21 : ByteArray, facePreviewInfoList : List<FacePreviewInfo>) {
        if (registerStatus == REGISTER_STATUS_READY && facePreviewInfoList.isNotEmpty()) {
            registerStatus = REGISTER_STATUS_PROCESSING
            Observable.create(ObservableOnSubscribe<Boolean> { emitter ->
                val success = FaceServer.instance.registerNv21(
                    nv21.clone(),
                    previewSize!!.width,
                    previewSize!!.height,
                    facePreviewInfoList[0].faceInfo,
                    "registered" + faceHelper!!.getTrackedFaceCount()
                )
                emitter.onNext(success)
            }).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Boolean> {
                    override fun onSubscribe(d: Disposable) {
                        TODO("Not yet implemented")
                    }

                    override fun onNext(t: Boolean) {
                        val result = if (t)
                            "register success!"
                        else
                            "register failed"
                        ToastUtils.showShortToast(result)
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                        ToastUtils.showShortToast("register failed!")
                        registerStatus = REGISTER_STATUS_DONE
                    }

                    override fun onComplete() {
                    }

                })
        }
    }

    fun searchFace(frFace : FaceFeature, requestId : Int) {
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
                    Log.d(TAG, "onNext: $t")
                    if (t.username == null){
                        requestFeatureStatusMap[requestId] = 2
                    }
                    if (t.similar > 0.8)
                        _recognizeResult.value = t
                }

                override fun onError(e: Throwable) {
                    Log.d(RecognizeFaceActivity.TAG, "onError: ${e.message}")
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

    fun stop() {
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
     * 延迟 FAIL_RETRY_INTERVAL 重新进行活体检测
     *
     * @param requestId 人脸ID
     */
    private fun retryLivenessDetectDelayed(requestId: Int) {
        Observable.timer(
            FAIL_RETRY_INTERVAL,
            TimeUnit.MILLISECONDS
        )
            .subscribe(object : Observer<Long?> {
                var disposable: Disposable? = null
                override fun onSubscribe(d: Disposable) {
                    disposable = d
                    delayFaceTaskCompositeDisposable.add(disposable!!)
                }

                fun onNext(aLong: Long?) {}
                override fun onError(e: Throwable) {
                    e.printStackTrace()
                }

                override fun onComplete() {
                    // 将该人脸状态置为UNKNOWN，帧回调处理时会重新进行活体检测
//                    if (livenessDetect) {
//                        faceHelper!!.setName(requestId, Integer.toString(requestId))
//                    }
                    livenessMap[requestId] = LivenessInfo.UNKNOWN
                    delayFaceTaskCompositeDisposable.remove(disposable!!)
                }

                override fun onNext(t: Long) {
                }
            })
    }

    /**
     * 延迟 FAIL_RETRY_INTERVAL 重新进行人脸识别
     *
     * @param requestId 人脸ID
     */
    private fun retryRecognizeDelayed(requestId: Int) {
        requestFeatureStatusMap[requestId] = 2
        Observable.timer(
            FAIL_RETRY_INTERVAL,
            TimeUnit.MILLISECONDS
        )
            .subscribe(object : Observer<Long?> {
                var disposable: Disposable? = null
                override fun onSubscribe(d: Disposable) {
                    disposable = d
                    delayFaceTaskCompositeDisposable.add(disposable!!)
                }

                fun onNext(aLong: Long?) {}
                override fun onError(e: Throwable) {
                    e.printStackTrace()
                }

                override fun onComplete() {
                    // 将该人脸特征提取状态置为FAILED，帧回调处理时会重新进行活体检测
//                    faceHelper!!.setName(requestId, requestId.toString())
                    requestFeatureStatusMap[requestId] = 3
                    delayFaceTaskCompositeDisposable.remove(disposable!!)
                }

                override fun onNext(t: Long) {
                }
            })
    }


    companion object {
        const val TAG = "FaceViewModel"

        private const val MAX_DETECT_NUM = 10

        /**
         *  当FR成功，活体未成功时，FR等待活体的时间
         */
        private const val WAIT_LIVENESS_INTERVAL = 100L

        /**
         *  失败重试间隔 ms
         */
        private const val FAIL_RETRY_INTERVAL = 100L

        /**
         * 失败重试次数
         */
        private const val MAX_RETRY_TIME = 3

    }

}