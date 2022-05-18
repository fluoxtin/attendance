package com.example.attendance.faceserver

import androidx.annotation.Nullable
import com.arcsoft.face.FaceFeature
import com.arcsoft.face.LivenessInfo

interface FaceListener {

    /**
     * 当出现异常时执行
     *
     * @param e 异常信息
     */
    fun onFail(e : Exception)

    /**
     * 请求人脸特征后的回调
     *
     * @param faceFeature 人脸特征数据
     * @param requestId   请求码
     * @param errorCode   错误码
     */
    fun onFeatureInfoGet(faceFeature : FaceFeature?, requestId : Int, errorCode : Int)

    /**
     * 请求活体检测后的回调
     *
     * @param livenessInfo 活体检测结果
     * @param requestId    请求码
     * @param errorCode    错误码
     */
    fun onFaceLivenessInfoGet(
        @Nullable livenessInfo: LivenessInfo?,
        requestId: Int,
        errorCode: Int?
    )


}