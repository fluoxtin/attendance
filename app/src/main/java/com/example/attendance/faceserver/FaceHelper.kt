package com.example.attendance.faceserver

import android.hardware.Camera
import android.util.Log
import com.arcsoft.face.*
import java.util.*
import java.util.concurrent.*


/**
 * 人脸操作辅助类
 */
class FaceHelper private constructor(builder: Builder) {
    /**
     * 人脸追踪引擎
     */
    private val ftEngine: FaceEngine?

    /**
     * 特征提取引擎
     */
    private val frEngine: FaceEngine?

    /**
     * 活体检测引擎
     */
    private val flEngine: FaceEngine?
    private val previewSize: Camera.Size?
    private var faceInfoList: MutableList<FaceInfo>? = ArrayList()

    /**
     * 特征提取线程池
     */
    private val frExecutor: ExecutorService

    /**
     * 活体检测线程池
     */
    private val flExecutor: ExecutorService

    /**
     * 特征提取线程队列
     */
    private var frThreadQueue: LinkedBlockingQueue<Runnable>? = null

    /**
     * 活体检测线程队列
     */
    private var flThreadQueue: LinkedBlockingQueue<Runnable>? = null
    private var faceListener: FaceListener?

    /**
     * 上次应用退出时，记录的该App检测过的人脸数了
     */
    private var trackedFaceCount = 0

    /**
     * 本次打开引擎后的最大faceId
     */
    private var currentMaxFaceId = 0
    private val currentTrackIdList: MutableList<Int> = ArrayList()
    private val facePreviewInfoList: MutableList<FacePreviewInfo> = ArrayList<FacePreviewInfo>()

    /**
     * 用于存储人脸对应的姓名，KEY为trackId，VALUE为name
     */
    private var nameMap: ConcurrentHashMap<Int, String>? = ConcurrentHashMap()

    /**
     * 请求获取人脸特征数据
     *
     * @param nv21     图像数据
     * @param faceInfo 人脸信息
     * @param width    图像宽度
     * @param height   图像高度
     * @param format   图像格式
     * @param trackId  请求人脸特征的唯一请求码，一般使用trackId
     */
    fun requestFaceFeature(
        nv21: ByteArray?,
        faceInfo: FaceInfo,
        width: Int,
        height: Int,
        format: Int,
        trackId: Int
    ) {
        if (faceListener != null) {
            if (frEngine != null && frThreadQueue!!.remainingCapacity() > 0) {
                frExecutor.execute(
                    FaceRecognizeRunnable(
                        nv21,
                        faceInfo,
                        width,
                        height,
                        format,
                        trackId
                    )
                )
            } else {
                faceListener!!.onFeatureInfoGet(null, trackId, ERROR_BUSY)
            }
        }
    }

    /**
     * 请求获取活体检测结果，需要传入活体的参数，以下参数同
     *
     * @param nv21         NV21格式的图像数据
     * @param faceInfo     人脸信息
     * @param width        图像宽度
     * @param height       图像高度
     * @param format       图像格式
     * @param trackId      请求人脸特征的唯一请求码，一般使用trackId
     * @param livenessType 活体检测类型
     */
    fun requestFaceLiveness(
        nv21: ByteArray?,
        faceInfo: FaceInfo,
        width: Int,
        height: Int,
        format: Int,
        trackId: Int
    ) {
        if (faceListener != null) {
            if (flEngine != null && flThreadQueue!!.remainingCapacity() > 0) {
                flExecutor.execute(
                    FaceLivenessDetectRunnable(
                        nv21,
                        faceInfo,
                        width,
                        height,
                        format,
                        trackId
                    )
                )
            } else {
                faceListener!!.onFaceLivenessInfoGet(null, trackId, ERROR_BUSY)
            }
        }
    }

    /**
     * 释放对象
     */
    fun release() {
        if (!frExecutor.isShutdown) {
            frExecutor.shutdownNow()
            frThreadQueue!!.clear()
        }
        if (!flExecutor.isShutdown) {
            flExecutor.shutdownNow()
            flThreadQueue!!.clear()
        }
        if (faceInfoList != null) {
            faceInfoList!!.clear()
        }
        if (frThreadQueue != null) {
            frThreadQueue!!.clear()
            frThreadQueue = null
        }
        if (flThreadQueue != null) {
            flThreadQueue!!.clear()
            flThreadQueue = null
        }
        if (nameMap != null) {
            nameMap!!.clear()
        }
        nameMap = null
        faceListener = null
        faceInfoList = null
    }

    /**
     * 处理帧数据
     *
     * @param nv21 相机预览回传的NV21数据
     * @return 实时人脸处理结果，封装添加了一个trackId，trackId的获取依赖于faceId，用于记录人脸序号并保存
     */
    fun onPreviewFrame(nv21: ByteArray?): List<FacePreviewInfo> {
        return if (faceListener != null) {
            if (ftEngine != null) {
                faceInfoList!!.clear()
                val ftStartTime = System.currentTimeMillis()
                val code = ftEngine.detectFaces(
                    nv21,
                    previewSize!!.width,
                    previewSize.height,
                    FaceEngine.CP_PAF_NV21,
                    faceInfoList
                )
                if (code != ErrorInfo.MOK) {
                    faceListener!!.onFail(Exception("ft failed,code is $code"))
                } else {
                    //                    Log.i(TAG, "onPreviewFrame: ft costTime = " + (System.currentTimeMillis() - ftStartTime) + "ms");
                }
                /*
                      * 若需要多人脸搜索，删除此行代码
                      */
//                TrackUtil.keepMaxFace(faceInfoList)
                refreshTrackId(faceInfoList)
            }
//            facePreviewInfoList.clear()
            for (i in faceInfoList!!.indices) {
                facePreviewInfoList.add(FacePreviewInfo(faceInfoList!![i], currentTrackIdList[i]))
            }
            facePreviewInfoList
        } else {
            facePreviewInfoList.clear()
            facePreviewInfoList
        }
    }

    /**
     * 人脸特征提取线程
     */
    inner class FaceRecognizeRunnable internal constructor(
        nv21Data: ByteArray?,
        faceInfo: FaceInfo,
        width: Int,
        height: Int,
        format: Int,
        trackId: Int
    ) :
        Runnable {
        private val faceInfo: FaceInfo
        private val width: Int
        private val height: Int
        private val format: Int
        private val trackId: Int
        private var nv21Data: ByteArray?
        override fun run() {
            if (faceListener != null && nv21Data != null) {
                if (frEngine != null) {
                    val faceFeature = FaceFeature()
                    val frStartTime = System.currentTimeMillis()
                    var frCode: Int
                    synchronized(frEngine) {
                        frCode = frEngine.extractFaceFeature(
                            nv21Data,
                            width,
                            height,
                            format,
                            faceInfo,
                            faceFeature
                        )
                    }
                    if (frCode == ErrorInfo.MOK) {
//                        Log.i(TAG, "run: fr costTime = " + (System.currentTimeMillis() - frStartTime) + "ms");
                        faceListener?.onFeatureInfoGet(faceFeature, trackId, frCode)
                    } else {
                        faceListener?.onFeatureInfoGet(null, trackId, frCode)
                        faceListener!!.onFail(Exception("fr failed errorCode is $frCode"))
                    }
                } else {
                    faceListener?.onFeatureInfoGet(null, trackId, ERROR_FR_ENGINE_IS_NULL)
                    faceListener!!.onFail(Exception("fr failed ,frEngine is null"))
                }
            }
            nv21Data = null
        }

        init {

            this.nv21Data = nv21Data
            this.faceInfo = FaceInfo(faceInfo)
            this.width = width
            this.height = height
            this.format = format
            this.trackId = trackId
        }
    }

    /**
     * 活体检测的线程
     */
    inner class FaceLivenessDetectRunnable(
        nv21Data: ByteArray?,
        faceInfo: FaceInfo,
        width: Int,
        height: Int,
        format: Int,
        trackId: Int,
    ) :
        Runnable {
        private val faceInfo: FaceInfo
        private val width: Int
        private val height: Int
        private val format: Int
        private val trackId: Int
        private var nv21Data: ByteArray?
        override fun run() {
            if (faceListener != null && nv21Data != null) {
                if (flEngine != null) {
                    val livenessInfoList: List<LivenessInfo> = ArrayList()
                    var flCode: Int
                    synchronized(flEngine) {
                         flCode = flEngine.process(
                                nv21Data,
                                width,
                                height,
                                format,
                                Arrays.asList(faceInfo),
                                FaceEngine.ASF_LIVENESS
                            )

                    }
                    if (flCode == ErrorInfo.MOK) {
                        flCode = flEngine.getLiveness(livenessInfoList)

                    }
                    if (flCode == ErrorInfo.MOK && livenessInfoList.isNotEmpty()) {
                        faceListener!!.onFaceLivenessInfoGet(livenessInfoList[0], trackId, flCode)
                    } else {
                        faceListener!!.onFaceLivenessInfoGet(null, trackId, flCode)
                        faceListener!!.onFail(Exception("fl failed errorCode is $flCode"))
                    }
                } else {
                    faceListener!!.onFaceLivenessInfoGet(null, trackId, ERROR_FL_ENGINE_IS_NULL)
                    faceListener!!.onFail(Exception("fl failed ,frEngine is null"))
                }
            }
            nv21Data = null
        }

        init {

            this.nv21Data = nv21Data
            this.faceInfo = FaceInfo(faceInfo)
            this.width = width
            this.height = height
            this.format = format
            this.trackId = trackId
        }
    }

    /**
     * 刷新trackId
     *
     * @param ftFaceList 传入的人脸列表
     */
    private fun refreshTrackId(ftFaceList: List<FaceInfo>?) {
        currentTrackIdList.clear()
        for (faceInfo in ftFaceList!!) {
            currentTrackIdList.add(faceInfo.faceId + trackedFaceCount)
        }
        if (ftFaceList.size > 0) {
            currentMaxFaceId = ftFaceList[ftFaceList.size - 1].faceId
        }

        //刷新nameMap
        clearLeftName(currentTrackIdList)
    }

    /**
     * 获取当前的最大trackID,可用于退出时保存
     *
     * @return 当前trackId
     */
    fun getTrackedFaceCount(): Int {
        // 引擎的人脸下标从0开始，因此需要+1
        return trackedFaceCount + currentMaxFaceId + 1
    }

    /**
     * 新增搜索成功的人脸
     *
     * @param trackId 指定的trackId
     * @param name    trackId对应的人脸
     */
    fun setName(trackId: Int, name: String) {
        if (nameMap != null) {
            nameMap!![trackId] = name
        }
    }

    fun getName(trackId: Int): String? {
        return if (nameMap == null) null else nameMap!![trackId]
    }

    /**
     * 清除map中已经离开的人脸
     *
     * @param trackIdList 最新的trackIdList
     */
    private fun clearLeftName(trackIdList: List<Int>) {
        val keys = nameMap!!.keys()
        while (keys.hasMoreElements()) {
            val value = keys.nextElement()
            if (!trackIdList.contains(value)) {
                nameMap!!.remove(value)
            }
        }
    }

    class Builder {
        var ftEngine: FaceEngine? = null
        var frEngine: FaceEngine? = null
        var flEngine: FaceEngine? = null
        var previewSize: Camera.Size? = null
        var faceListener: FaceListener? = null
        var frQueueSize = 0
        var flQueueSize = 0
        var trackedFaceCount = 0
        fun ftEngine(engine: FaceEngine?): Builder {
            ftEngine = engine
            return this
        }

        fun frEngine(engine: FaceEngine?): Builder {
            frEngine = engine
            return this
        }

        fun flEngine(engine: FaceEngine?): Builder {
            flEngine = engine
            return this
        }

        fun previewSize(size : Camera.Size?): Builder {
            previewSize = size
            return this
        }

        fun faceListener(listener: FaceListener?): Builder {
            faceListener = listener
            return this
        }

        fun frQueueSize(size: Int): Builder {
            frQueueSize = size
            return this
        }

        fun flQueueSize(size: Int): Builder {
            flQueueSize = size
            return this
        }

        fun trackedFaceCount(count: Int): Builder {
            trackedFaceCount = count
            return this
        }

        fun build(): FaceHelper {
            return FaceHelper(this)
        }
    }

    companion object {
        private const val TAG = "FaceHelper"

        /**
         * 线程池正在处理任务
         */
        private const val ERROR_BUSY = -1

        /**
         * 特征提取引擎为空
         */
        private const val ERROR_FR_ENGINE_IS_NULL = -2

        /**
         * 活体检测引擎为空
         */
        private const val ERROR_FL_ENGINE_IS_NULL = -3
    }

    init {
        ftEngine = builder.ftEngine
        faceListener = builder.faceListener
        trackedFaceCount = builder.trackedFaceCount
        previewSize = builder.previewSize
        frEngine = builder.frEngine
        flEngine = builder.flEngine
        /**
         * fr 线程队列大小
         */
        var frQueueSize = 5
        if (builder.frQueueSize > 0) {
            frQueueSize = builder.frQueueSize
        } else {
            Log.e(
                TAG,
                "frThread num must > 0,now using default value:$frQueueSize"
            )
        }
        frThreadQueue = LinkedBlockingQueue(frQueueSize)
        frExecutor = ThreadPoolExecutor(1, frQueueSize, 0, TimeUnit.MILLISECONDS, frThreadQueue)
        /**
         * fl 线程队列大小
         */
        var flQueueSize = 5
        if (builder.flQueueSize > 0) {
            flQueueSize = builder.flQueueSize
        } else {
            Log.e(
                TAG,
                "flThread num must > 0,now using default value:$flQueueSize"
            )
        }
        flThreadQueue = LinkedBlockingQueue(flQueueSize)
        flExecutor = ThreadPoolExecutor(1, flQueueSize, 0, TimeUnit.MILLISECONDS, flThreadQueue)
        if (previewSize == null) {
            throw RuntimeException("previewSize must be specified!")
        }
    }
}
