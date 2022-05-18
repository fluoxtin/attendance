package com.example.attendance.faceserver

import com.arcsoft.face.FaceInfo

data class FacePreviewInfo(
    val faceInfo: FaceInfo,
    val trackId : Int? = null
)
