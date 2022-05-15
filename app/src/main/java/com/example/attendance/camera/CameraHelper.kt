package com.example.attendance.camera

import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.PreviewCallback
import android.util.Log
import android.view.*
import android.view.TextureView.SurfaceTextureListener
import java.io.IOException
import java.util.*
import kotlin.math.abs

/**
 *  获取 nv21 数据等操作
 */

class CameraHelper private constructor(builder: Builder) : PreviewCallback {
    private var mCamera: Camera? = null
    private var mCameraId = 0
    private var previewViewSize: Point?
    private var previewDisplayView: View?
    private var previewSize: Camera.Size? = null
    private var specificPreviewSize: Point?
    private var displayOrientation = 0
    private var rotation: Int
    private var additionalRotation: Int
    private var isMirror = false
    private var specificCameraId: Int? = null
    private var cameraListener: CameraListener?

    fun init() {
        if (previewDisplayView is TextureView) {
            (previewDisplayView as TextureView?)!!.surfaceTextureListener = textureListener
        } else if (previewDisplayView is SurfaceView) {
            (previewDisplayView as SurfaceView).holder.addCallback(surfaceCallback)
        }
        if (isMirror) {
            previewDisplayView!!.scaleX = -1f
        }
    }

    fun start() {
        synchronized(this) {
            if (mCamera != null) {
                return
            }
            //相机数量为2则打开1,1则打开0,相机ID 1为前置，0为后置
            mCameraId = Camera.getNumberOfCameras() - 1
            //若指定了相机ID且该相机存在，则打开指定的相机
            specificCameraId?.apply {
                if (this < mCameraId)
                    mCameraId = this
            }

            //没有相机
            if (mCameraId == -1) {
                if (cameraListener != null) {
                    cameraListener!!.onCameraError(Exception("camera not found"))
                }
                return
            }
            if (mCamera == null) {
                mCamera = Camera.open(mCameraId)
            }
            displayOrientation = getCameraOri(rotation)
            mCamera!!.setDisplayOrientation(displayOrientation)
            try {
                val parameters: Camera.Parameters = mCamera!!.parameters
                parameters.previewFormat = ImageFormat.NV21

                //预览大小设置
                previewSize = parameters.previewSize
                val supportedPreviewSizes: List<Camera.Size>? =
                    parameters.supportedPreviewSizes
                if (supportedPreviewSizes != null && supportedPreviewSizes.size > 0) {
                    previewSize = getBestSupportedSize(supportedPreviewSizes, previewViewSize)
                }
                parameters.setPreviewSize(previewSize!!.width, previewSize!!.height)

                //对焦模式设置
                val supportedFocusModes: List<String>? =
                    parameters.supportedFocusModes
                if (supportedFocusModes != null && supportedFocusModes.isNotEmpty()) {
                    when {
                        supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) -> {
                            parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                        }
                        supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) -> {
                            parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
                        }
                        supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO) -> {
                            parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
                        }
                    }
                }
                mCamera!!.parameters = parameters
                if (previewDisplayView is TextureView) {
                    mCamera!!.setPreviewTexture((previewDisplayView as TextureView).surfaceTexture)
                } else {
                    mCamera!!.setPreviewDisplay((previewDisplayView as SurfaceView?)!!.holder)
                }
                mCamera!!.setPreviewCallback(this)
                mCamera!!.startPreview()
                if (cameraListener != null) {
                    cameraListener!!.onCameraOpened(
                        mCamera!!,
                        mCameraId,
                        displayOrientation,
                        isMirror
                    )
                }
            } catch (e: Exception) {
                if (cameraListener != null) {
                    cameraListener!!.onCameraError(e)
                }
            }
        }
    }

    private fun getCameraOri(rotation: Int): Int {
        var degrees = rotation * 90
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
            else -> {}
        }
        additionalRotation /= 90
        additionalRotation *= 90
        degrees += additionalRotation
        var result: Int
        val info = CameraInfo()
        Camera.getCameraInfo(mCameraId, info)
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360
        } else {
            result = (info.orientation - degrees + 360) % 360
        }
        return result
    }

    fun stop() {
        synchronized(this) {
            if (mCamera == null) {
                return
            }
            mCamera!!.setPreviewCallback(null)
            mCamera!!.stopPreview()
            mCamera!!.release()
            mCamera = null
            if (cameraListener != null) {
                cameraListener!!.onCameraClosed()
            }
        }
    }

    val isStopped: Boolean
        get() {
            synchronized(this) { return mCamera == null }
        }

    fun release() {
        synchronized(this) {
            stop()
            previewDisplayView = null
            specificCameraId = null
            cameraListener = null
            previewViewSize = null
            specificPreviewSize = null
            previewSize = null
        }
    }

    private fun getBestSupportedSize(
        sizes: List<Camera.Size>,
        previewViewSize: Point?
    ): Camera.Size {
        var sizes: List<Camera.Size>? = sizes
        if (sizes == null || sizes.isEmpty()) {
            return mCamera!!.parameters.previewSize
        }
        val tempSizes = sizes.toTypedArray()
        Arrays.sort(tempSizes) { o1, o2 ->

            //            override fun compare(o1: Camera.Size, o2: Camera.Size): Int {
            //                return if (o1.width > o2.width) {
            //                    -1
            //                } else if (o1.width == o2.width) {
            //                    if (o1.height > o2.height) -1 else 1
            //                } else {
            //                    1
            //                }
            //            }
            if (o1.width > o2.width) {
                -1
            } else if (o1.width == o2.width) {
                if (o1.height > o2.height) -1 else 1
            } else {
                1
            }
        }
        sizes = listOf(*tempSizes)
        var bestSize = sizes[0]
        var previewViewRatio: Float
        previewViewRatio = if (previewViewSize != null) {
            previewViewSize.x.toFloat() / previewViewSize.y.toFloat()
        } else {
            bestSize.width.toFloat() / bestSize.height.toFloat()
        }
        if (previewViewRatio > 1) {
            previewViewRatio = 1 / previewViewRatio
        }
        val isNormalRotate = additionalRotation % 180 == 0
        for (s in sizes) {
            if (specificPreviewSize != null && specificPreviewSize!!.x == s.width && specificPreviewSize!!.y == s.height) {
                return s
            }
            if (isNormalRotate) {
                if (abs(s.height / s.width.toFloat() - previewViewRatio) < abs(bestSize.height / bestSize.width.toFloat() - previewViewRatio)) {
                    bestSize = s
                }
            } else {
                if (abs(s.width / s.height.toFloat() - previewViewRatio) < abs(bestSize.width / bestSize.height.toFloat() - previewViewRatio)) {
                    bestSize = s
                }
            }
        }
        return bestSize
    }

    val supportedPreviewSizes: List<Camera.Size>?
        get() = if (mCamera == null) {
            null
        } else mCamera!!.parameters.supportedPreviewSizes
    val supportedPictureSizes: List<Camera.Size>?
        get() {
            return if (mCamera == null) {
                null
            } else mCamera!!.parameters.supportedPictureSizes
        }

    override fun onPreviewFrame(nv21: ByteArray, camera: Camera) {
        if (cameraListener != null) {
            cameraListener!!.onPreview(nv21, camera)
        }
    }

    private val textureListener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            surfaceTexture: SurfaceTexture,
            width: Int,
            height: Int
        ) {
//            start();
            if (mCamera != null) {
                try {
                    mCamera!!.setPreviewTexture(surfaceTexture)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        override fun onSurfaceTextureSizeChanged(
            surfaceTexture: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            Log.i(
                TAG,
                "onSurfaceTextureSizeChanged: $width  $height"
            )
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            stop()
            return false
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
    }
    private val surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
//            start();
            if (mCamera != null) {
                try {
                    mCamera!!.setPreviewDisplay(holder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            stop()
        }
    }

    fun changeDisplayOrientation(rotation: Int) {
        if (mCamera != null) {
            this.rotation = rotation
            displayOrientation = getCameraOri(rotation)
            mCamera!!.setDisplayOrientation(displayOrientation)
            if (cameraListener != null) {
                cameraListener!!.onCameraConfigurationChanged(mCameraId, displayOrientation)
            }
        }
    }

    fun switchCamera(): Boolean {
        if (Camera.getNumberOfCameras() < 2) {
            return false
        }
        // cameraId ,0为后置，1为前置
        specificCameraId = 1 - mCameraId
        stop()
        start()
        return true
    }

    class Builder {
        /**
         * 预览显示的view，目前仅支持surfaceView和textureView
         */
        var previewDisplayView: View? = null

        /**
         * 是否镜像显示，只支持textureView
         */
        var isMirror = false

        /**
         * 指定的相机ID
         */
        var specificCameraId: Int? = null

        /**
         * 事件回调
         */
        var cameraListener: CameraListener? = null

        /**
         * 屏幕的长宽，在选择最佳相机比例时用到
         */
        var previewViewSize: Point? = null

        /**
         * 传入getWindowManager().getDefaultDisplay().getRotation()的值即可
         */
        var rotation = 0

        /**
         * 指定的预览宽高，若系统支持则会以这个预览宽高进行预览
         */
        var previewSize: Point? = null

        /**
         * 额外的旋转角度（用于适配一些定制设备）
         */
        var additionalRotation = 0
        fun previewOn(view : View?): Builder {
            return if (view is SurfaceView || view is TextureView) {
                previewDisplayView = view
                this
            } else {
                throw RuntimeException("you must preview on a textureView or a surfaceView")
            }
        }

        fun isMirror(value : Boolean): Builder {
            isMirror = value
            return this
        }

        fun previewSize(value: Point?): Builder {
            previewSize = value
            return this
        }

        fun previewViewSize(value: Point?): Builder {
            previewViewSize = value
            return this
        }

        fun rotation(value: Int): Builder {
            rotation = value
            return this
        }

        fun additionalRotation(value: Int): Builder {
            additionalRotation = value
            return this
        }

        fun specificCameraId(value: Int?): Builder {
            specificCameraId = value
            return this
        }

        fun cameraListener(value: CameraListener?): Builder {
            cameraListener = value
            return this
        }

        fun build(): CameraHelper {
            if (previewViewSize == null) {
                Log.e(TAG, "previewViewSize is null, now use default previewSize")
            }
            if (cameraListener == null) {
                Log.e(TAG, "cameraListener is null, callback will not be called")
            }
            if (previewDisplayView == null) {
                throw RuntimeException("you must preview on a textureView or a surfaceView")
            }
            return CameraHelper(this)
        }
    }

    companion object {
        private const val TAG = "CameraHelper"
    }

    init {
        previewDisplayView = builder.previewDisplayView
        specificCameraId = builder.specificCameraId
        cameraListener = builder.cameraListener
        rotation = builder.rotation
        additionalRotation = builder.additionalRotation
        previewViewSize = builder.previewViewSize
        specificPreviewSize = builder.previewSize
        if (builder.previewDisplayView is TextureView) {
            isMirror = builder.isMirror
        } else if (isMirror) {
            throw RuntimeException("mirror is effective only when the preview is on a textureView")
        }
    }
}