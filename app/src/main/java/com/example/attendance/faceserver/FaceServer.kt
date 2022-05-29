package com.example.attendance.faceserver

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
import com.arcsoft.face.*
import com.arcsoft.face.enums.DetectFaceOrientPriority
import com.arcsoft.face.enums.DetectMode
import com.arcsoft.imageutil.ArcSoftImageFormat
import com.arcsoft.imageutil.ArcSoftImageUtil
import com.arcsoft.imageutil.ArcSoftImageUtilError
import com.arcsoft.imageutil.ArcSoftRotateDegree
import com.example.attendance.App
import com.example.attendance.oss.OSSUploader
import com.example.attendance.util.ToastUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/**
 * 人脸库操作类，包含注册和搜索
 */
class FaceServer {
    /**
     * 是否正在搜索人脸，保证搜索操作单线程进行
     */
    private var isProcessing = false

    /**
     * 初始化
     *
     * @param context 上下文对象
     * @return 是否初始化成功
     */
    fun init(context: Context?): Boolean {
        synchronized(this) {
            if (faceEngine == null && context != null) {
                faceEngine = FaceEngine()
                val engineCode: Int = faceEngine!!.init(
                    context,
                    DetectMode.ASF_DETECT_MODE_IMAGE,
                    DetectFaceOrientPriority.ASF_OP_0_ONLY,
                    16,
                    1,
                    FaceEngine.ASF_FACE_RECOGNITION or FaceEngine.ASF_FACE_DETECT
                )
                return if (engineCode == ErrorInfo.MOK) {
                    initFaceList(context)
                    true
                } else {
                    faceEngine = null
                    Log.e(TAG, "init: failed! code = $engineCode")
                    false
                }
            }
            return false
        }
    }

    /**
     * 销毁
     */
    fun unInit() {
        synchronized(this) {
            if (faceRegisterInfoList != null) {
                faceRegisterInfoList = null
                faceRegisterInfoList = null
            }
            if (faceEngine != null) {
                faceEngine!!.unInit()
                faceEngine = null
            }
        }
    }

    /**
     * 初始化人脸特征数据以及人脸特征数据对应的注册图
     *
     * @param context 上下文对象
     */
    private fun initFaceList(context: Context) {
        synchronized(this) {
            if (ROOT_PATH == null) {
                ROOT_PATH = context.filesDir.absolutePath
            }
            val featureDir =
                File(ROOT_PATH + File.separator.toString() + SAVE_FEATURE_DIR)
            if (!featureDir.exists() || !featureDir.isDirectory) {
                return
            }
            val featureFiles: Array<File> = featureDir.listFiles()
            if (featureFiles.isEmpty()) {
                return
            }
            faceRegisterInfoList = ArrayList()
            for (featureFile in featureFiles) {
                try {
                    val fis = FileInputStream(featureFile)
                    val feature = ByteArray(FaceFeature.FEATURE_SIZE)
                    fis.read(feature)
                    fis.close()
                    faceRegisterInfoList?.add(
                        FaceRegisterInfo(
                            feature,
                            featureFile.name
                        )
                    )
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            Log.d(TAG, "initFaceList: size : ${faceRegisterInfoList?.size}")
        }
    }

    fun getFaceNumber(context: Context?): Int {
        synchronized(this) {
            if (context == null) {
                return 0
            }
            if (ROOT_PATH == null) {
                ROOT_PATH = context.filesDir.absolutePath
            }
            val featureFileDir =
                File(ROOT_PATH + File.separator.toString() + SAVE_FEATURE_DIR)
            var featureCount = 0
            if (featureFileDir.exists() && featureFileDir.isDirectory) {
                val featureFiles: Array<out String>? = featureFileDir.list()
                if (featureFiles != null) {
                    featureCount = featureFiles.size
                }
            }
            var imageCount = 0
            val imgFileDir =
                File(ROOT_PATH + File.separator.toString() + SAVE_IMG_DIR)
            if (imgFileDir.exists() && imgFileDir.isDirectory) {
                val imageFiles: Array<out String>? = imgFileDir.list()
                if (imageFiles != null) {
                    imageCount = imageFiles.size
                }
            }
            return if (featureCount > imageCount) imageCount else featureCount
        }
    }

    fun clearAllFaces(context: Context?): Int {
        synchronized(this) {
            if (context == null) {
                return 0
            }
            if (ROOT_PATH == null) {
                ROOT_PATH = context.filesDir.absolutePath
            }
            if (faceRegisterInfoList != null) {
                faceRegisterInfoList = null
            }
            val featureFileDir =
                File(ROOT_PATH + File.separator.toString() + SAVE_FEATURE_DIR)
            var deletedFeatureCount = 0
            if (featureFileDir.exists() && featureFileDir.isDirectory) {
                val featureFiles: Array<out File>? = featureFileDir.listFiles()
                if (featureFiles != null) {
                    if (featureFiles.isNotEmpty()) {
                        for (featureFile in featureFiles) {
                            if (featureFile.delete()) {
                                deletedFeatureCount++
                            }
                        }
                    }
                }
            }
            var deletedImageCount = 0
            val imgFileDir =
                File(ROOT_PATH + File.separator.toString() + SAVE_IMG_DIR)
            if (imgFileDir.exists() && imgFileDir.isDirectory) {
                val imgFiles: Array<out File>? = imgFileDir.listFiles()
                if (imgFiles != null) {
                    if (imgFiles.isNotEmpty()) {
                        for (imgFile in imgFiles) {
                            if (imgFile.delete()) {
                                deletedImageCount++
                            }
                        }
                    }
                }
            }
            return if (deletedFeatureCount > deletedImageCount) deletedImageCount else deletedFeatureCount
        }
    }

    /**
     * 用于预览时注册人脸
     *
     * @param context  上下文对象
     * @param nv21     NV21数据
     * @param width    NV21宽度
     * @param height   NV21高度
     * @param faceInfo [FaceEngine.detectFaces]获取的人脸信息
     * @param name     保存的名字，若为空则使用时间戳
     * @return 是否注册成功
     */
    fun registerNv21(
        nv21: ByteArray?,
        width: Int,
        height: Int,
        faceInfo: FaceInfo,
        name: String?
    ): Boolean {
        synchronized(this) {
            if (faceEngine == null ||
                nv21 == null ||
                width % 4 != 0 ||
                nv21.size != width * height * 3 / 2
            ) {
                Log.e(TAG, "registerNv21: invalid params")
                return false
            }
            if (ROOT_PATH == null) {
                App.getInstance().apply {
                    ROOT_PATH = filesDir.absolutePath
                }
            }
            //特征存储的文件夹
            val featureDir =
                File(ROOT_PATH + File.separator.toString() + SAVE_FEATURE_DIR)
            if (!featureDir.exists() && !featureDir.mkdirs()) {
                Log.e(TAG, "registerNv21: can not create feature directory")
                return false
            }
            //图片存储的文件夹
            val imgDir =
                File(ROOT_PATH + File.separator.toString() + SAVE_IMG_DIR)
            if (!imgDir.exists() && !imgDir.mkdirs()) {
                Log.e(TAG, "registerNv21: can not create image directory")
                return false
            }
            val faceFeature = FaceFeature()
            //特征提取
            val code: Int = faceEngine!!.extractFaceFeature(
                nv21,
                width,
                height,
                FaceEngine.CP_PAF_NV21,
                faceInfo,
                faceFeature
            )
            return if (code != ErrorInfo.MOK) {
                Log.e(
                    TAG,
                    "registerNv21: extractFaceFeature failed , code is $code"
                )
                false
            } else {
                val userName =
                    name ?: System.currentTimeMillis().toString()
                try {
                    // 保存注册结果（注册图、特征数据）
                    // 为了美观，扩大rect截取注册图
                    val cropRect: Rect? =
                        getBestRect(width, height, faceInfo.rect)
                    if (cropRect == null) {
                        Log.e(TAG, "registerNv21: cropRect is null!")
                        return false
                    }
                    cropRect.left = cropRect.left and 3.inv()
                    cropRect.top = cropRect.top and 3.inv()
                    cropRect.right = cropRect.right and 3.inv()
                    cropRect.bottom = cropRect.bottom and 3.inv()
                    val file =
                        File("$imgDir${File.separator}$userName$IMG_SUFFIX")


                    // 创建一个头像的Bitmap，存放旋转结果图
                    val headBmp: Bitmap = getHeadImage(
                        nv21,
                        width,
                        height,
                        faceInfo.orient,
                        cropRect,
                        ArcSoftImageFormat.NV21
                    )
                    val fosImage = FileOutputStream(file)
                    headBmp.compress(Bitmap.CompressFormat.JPEG, 100, fosImage)
                    fosImage.close()
                    val fosFeature =
                        FileOutputStream("$featureDir${File.separator}$userName")
                    fosFeature.write(faceFeature.featureData)
                    fosFeature.close()

                    //内存中的数据同步
                    if (faceRegisterInfoList == null) {
                        faceRegisterInfoList = ArrayList()
                    }


                    faceRegisterInfoList?.add(
                        FaceRegisterInfo(
                            faceFeature.featureData,
                            userName
                        )
                    )
                    true
                } catch (e: IOException) {
                    e.printStackTrace()
                    false
                }
            }
        }
    }

    /**
     * 用于注册照片人脸
     *
     * @param context 上下文对象
     * @param bgr24   bgr24数据
     * @param width   bgr24宽度
     * @param height  bgr24高度
     * @param name    保存的名字，若为空则使用时间戳
     * @return 是否注册成功
     */
    fun registerBgr24(
        context: Context?,
        bgr24: ByteArray?,
        width: Int,
        height: Int,
        name: String?
    ): Boolean {
        synchronized(this) {
            if (faceEngine == null || context == null || bgr24 == null || width % 4 != 0 || bgr24.size != width * height * 3) {
                Log.e(TAG, "registerBgr24:  invalid params")
                return false
            }
            if (ROOT_PATH == null) {
                ROOT_PATH = context.filesDir.absolutePath
            }
            //特征存储的文件夹
            val featureDir =
                File(ROOT_PATH + File.separator.toString() + SAVE_FEATURE_DIR)
            Log.d(TAG, "特征存储文件夹 : ${featureDir.absolutePath} ")
            if (!featureDir.exists() && !featureDir.mkdirs()) {
                Log.e(TAG, "registerBgr24: can not create feature directory")
                return false
            }
            //图片存储的文件夹
            val imgDir =
                File(ROOT_PATH + File.separator.toString() + SAVE_IMG_DIR)
            Log.d(TAG, "图片存储文件夹 : ${imgDir.absolutePath}")
            if (!imgDir.exists() && !imgDir.mkdirs()) {
                Log.e(TAG, "registerBgr24: can not create image directory")
                return false
            }
            //人脸检测
            val faceInfoList: List<FaceInfo> = ArrayList()
            var code: Int = faceEngine!!.detectFaces(
                bgr24,
                width,
                height,
                FaceEngine.CP_PAF_BGR24,
                faceInfoList
            )
            return if (code == ErrorInfo.MOK && faceInfoList.isNotEmpty()) {
                val faceFeature = FaceFeature()

                //特征提取
                code = faceEngine!!.extractFaceFeature(
                    bgr24,
                    width,
                    height,
                    FaceEngine.CP_PAF_BGR24,
                    faceInfoList[0],
                    faceFeature
                )
                val userName =
                    name ?: System.currentTimeMillis().toString()
                try {
                    //保存注册结果（注册图、特征数据）
                    if (code == ErrorInfo.MOK) {
                        //为了美观，扩大rect截取注册图
                        val cropRect: Rect? = getBestRect(
                            width,
                            height,
                            faceInfoList[0].rect
                        )
                        if (cropRect == null) {
                            Log.e(TAG, "registerBgr24: cropRect is null")
                            return false
                        }
                        cropRect.left = cropRect.left and 3.inv()
                        cropRect.top = cropRect.top and 3.inv()
                        cropRect.right = cropRect.right and 3.inv()
                        cropRect.bottom = cropRect.bottom and 3.inv()
                        val file =
                            File("$imgDir${File.separator}$userName$IMG_SUFFIX")
                        val fosImage = FileOutputStream(file)
                        Log.d(TAG, "fosImage path : ${file.absolutePath}")

                        // 创建一个头像的Bitmap，存放旋转结果图
                        val headBmp: Bitmap = getHeadImage(
                            bgr24,
                            width,
                            height,
                            faceInfoList[0].orient,
                            cropRect,
                            ArcSoftImageFormat.BGR24
                        )
                        // 保存到本地
                        headBmp.compress(Bitmap.CompressFormat.JPEG, 100, fosImage)
                        fosImage.close()

//                        val upload = OSSUploader()
//                        upload.uploadFile("$userName.jpg", file.absolutePath)

                        // 保存特征数据
                        val fosFeature =
                            FileOutputStream("$featureDir${File.separator}$userName")
                        fosFeature.write(faceFeature.featureData)
                        fosFeature.close()

                        // 内存中的数据同步
                        if (faceRegisterInfoList == null) {
                            faceRegisterInfoList = ArrayList()
                        }
                        faceRegisterInfoList?.add(
                            FaceRegisterInfo(
                                faceFeature.featureData,
                                userName
                            )
                        )
                        true
                    } else {
                        Log.e(
                            TAG,
                            "registerBgr24: extract face feature failed, code is $code"
                        )
                        false
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    false
                }
            } else {
                Log.e(TAG, "registerBgr24: no face detected, code is $code")
                false
            }
        }
    }

    fun registerByByteArray(byteArray: ByteArray, name: String) {
        var bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        bitmap = ArcSoftImageUtil.getAlignedBitmap(bitmap, true)
        val bgr24 = ArcSoftImageUtil
            .createImageData(bitmap.width, bitmap.height, ArcSoftImageFormat.BGR24)
        val transformCode = ArcSoftImageUtil
            .bitmapToImageData(bitmap, bgr24, ArcSoftImageFormat.BGR24)
        if (transformCode != ArcSoftImageUtilError.CODE_SUCCESS) {
            Log.e(TAG, "transformCode Error ")
            return
        }
        val success = registerBgr24(
                App.getInstance(),
                bgr24,
                bitmap.width,
                bitmap.height,
                name
            )
        if (success)
            Log.d(TAG, "register success")
    }

    /**
     * 截取合适的头像并旋转，保存为注册头像
     *
     * @param originImageData 原始的BGR24数据
     * @param width           BGR24图像宽度
     * @param height          BGR24图像高度
     * @param orient          人脸角度
     * @param cropRect        裁剪的位置
     * @param imageFormat     图像格式
     * @return 头像的图像数据
     */
    private fun getHeadImage(
        originImageData: ByteArray,
        width: Int,
        height: Int,
        orient: Int,
        cropRect: Rect,
        imageFormat: ArcSoftImageFormat
    ): Bitmap {
        val headImageData: ByteArray =
            ArcSoftImageUtil.createImageData(cropRect.width(), cropRect.height(), imageFormat)
        val cropCode: Int = ArcSoftImageUtil.cropImage(
            originImageData,
            headImageData,
            width,
            height,
            cropRect,
            imageFormat
        )
        if (cropCode != ArcSoftImageUtilError.CODE_SUCCESS) {
            throw RuntimeException("crop image failed, code is $cropCode")
        }

        //判断人脸旋转角度，若不为0度则旋转注册图
        var rotateHeadImageData: ByteArray? = null
        val rotateCode: Int
        val cropImageWidth : Int
        val cropImageHeight : Int
        // 90度或270度的情况，需要宽高互换
        if (orient == FaceEngine.ASF_OC_90 || orient == FaceEngine.ASF_OC_270) {
            cropImageWidth = cropRect.height()
            cropImageHeight = cropRect.width()
        } else {
            cropImageWidth = cropRect.width()
            cropImageHeight = cropRect.height()
        }
        var rotateDegree: ArcSoftRotateDegree? = null
        when (orient) {
            FaceEngine.ASF_OC_90 -> rotateDegree = ArcSoftRotateDegree.DEGREE_270
            FaceEngine.ASF_OC_180 -> rotateDegree = ArcSoftRotateDegree.DEGREE_180
            FaceEngine.ASF_OC_270 -> rotateDegree = ArcSoftRotateDegree.DEGREE_90
            FaceEngine.ASF_OC_0 -> rotateHeadImageData = headImageData
            else -> rotateHeadImageData = headImageData
        }
        // 非0度的情况，旋转图像
        if (rotateDegree != null) {
            rotateHeadImageData = ByteArray(headImageData.size)
            rotateCode = ArcSoftImageUtil.rotateImage(
                headImageData,
                rotateHeadImageData,
                cropRect.width(),
                cropRect.height(),
                rotateDegree,
                imageFormat
            )
            if (rotateCode != ArcSoftImageUtilError.CODE_SUCCESS) {
                throw RuntimeException("rotate image failed, code is $rotateCode")
            }
        }
        // 将创建一个Bitmap，并将图像数据存放到Bitmap中
        val headBmp: Bitmap =
            Bitmap.createBitmap(cropImageWidth, cropImageHeight, Bitmap.Config.RGB_565)
        if (ArcSoftImageUtil.imageDataToBitmap(
                rotateHeadImageData,
                headBmp,
                imageFormat
            ) != ArcSoftImageUtilError.CODE_SUCCESS
        ) {
            throw RuntimeException("failed to transform image data to bitmap")
        }
        return headBmp
    }

    /**
     * 在特征库中搜索
     *
     * @param faceFeature 传入特征数据
     * @return 比对结果
     */
    fun getTopOfFaceLib(faceFeature: FaceFeature?): CompareResult? {
        if (faceEngine == null ||
            isProcessing ||
            faceFeature == null ||
            faceRegisterInfoList == null ||
            faceRegisterInfoList!!.size == 0
        ) {
            return null
        }
        val tempFaceFeature = FaceFeature()
        val faceSimilar = FaceSimilar()
        var maxSimilar = 0f
        var maxSimilarIndex = -1
        isProcessing = true
        for (i in 0 until faceRegisterInfoList!!.size) {
            tempFaceFeature.featureData = faceRegisterInfoList!![i].featureData
            faceEngine!!.compareFaceFeature(faceFeature, tempFaceFeature, faceSimilar)
            if (faceSimilar.score > maxSimilar) {
                maxSimilar = faceSimilar.score
                maxSimilarIndex = i
            }
        }
        isProcessing = false
        return if (maxSimilarIndex != -1) {
            CompareResult(
                faceRegisterInfoList!![maxSimilarIndex].username,
                maxSimilar
            )
        } else null
    }

    companion object {
        private const val TAG = "FaceServer"
        const val IMG_SUFFIX = ".jpg"
        private var faceEngine: FaceEngine? = null
        private var faceServer: FaceServer? = null
        private var faceRegisterInfoList: MutableList<FaceRegisterInfo>? = null
        var ROOT_PATH: String? = null

        /**
         * 存放注册图的目录
         */
        val SAVE_IMG_DIR = "register" + File.separator.toString() + "imgs"

        /**
         * 存放特征的目录
         */
        private val SAVE_FEATURE_DIR = "register" + File.separator.toString() + "features"
        val instance: FaceServer
            get() {
                if (faceServer == null) {
                    synchronized(FaceServer::class.java) {
                        if (faceServer == null) {
                            faceServer = FaceServer()
                        }
                    }
                }
                return faceServer!!
            }

        /**
         * 将图像中需要截取的Rect向外扩张一倍，若扩张一倍会溢出，则扩张到边界，若Rect已溢出，则收缩到边界
         *
         * @param width   图像宽度
         * @param height  图像高度
         * @param srcRect 原Rect
         * @return 调整后的Rect
         */
        private fun getBestRect(width: Int, height: Int, srcRect: Rect?): Rect? {
            if (srcRect == null) {
                return null
            }
            val rect = Rect(srcRect)

            // 原rect边界已溢出宽高的情况
            val maxOverFlow: Int = max(
                -rect.left,
                max(-rect.top, max(rect.right - width, rect.bottom - height))
            )
            if (maxOverFlow >= 0) {
                rect.inset(maxOverFlow, maxOverFlow)
                return rect
            }

            // 原rect边界未溢出宽高的情况
            var padding: Int = rect.height() / 2

            // 若以此padding扩张rect会溢出，取最大padding为四个边距的最小值
            if (!(rect.left - padding > 0 &&
                        rect.right + padding < width &&
                        rect.top - padding > 0 &&
                        rect.bottom + padding < height)
            ) {
                padding = min(
                    min(
                        min(rect.left, width - rect.right),
                        height - rect.bottom
                    ), rect.top
                )
            }
            rect.inset(-padding, -padding)
            return rect
        }
    }
}