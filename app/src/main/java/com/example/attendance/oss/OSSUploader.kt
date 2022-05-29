package com.example.attendance.oss

import android.util.Log
import com.alibaba.sdk.android.oss.*
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback
import com.alibaba.sdk.android.oss.common.auth.OSSAuthCredentialsProvider
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider
import com.alibaba.sdk.android.oss.model.*
import com.example.attendance.App
import com.example.attendance.faceserver.FaceServer
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class OSSUploader {

    init {
        val provider = OSSPlainTextAKSKCredentialProvider(Config.ACCESS_KEY, Config.SECRET_KEY)
        val conf = ClientConfiguration()
        conf.connectionTimeout = 15 * 1000
        conf.socketTimeout = 15 * 1000
        conf.maxConcurrentRequest = 5
        conf.maxErrorRetry = 2
        oss = OSSClient(App.getInstance(), Config.OSS_ENDPOINT, provider, conf)
    }

    fun uploadFile(name : String, localPath : String) {
        val put = PutObjectRequest(Config.BUCKET_NAME, name, localPath)
//
        oss?.apply { 
            val task = asyncPutObject(put, object : OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
                override fun onSuccess(request: PutObjectRequest?, result: PutObjectResult?) {
                    Log.d(TAG, "onSuccess: upload ")

                }

                override fun onFailure(
                    request: PutObjectRequest?,
                    clientException: ClientException?,
                    serviceException: ServiceException?
                ) {
                    Log.d(TAG, "onFailure: ")
                    Log.d(TAG, "onFailure: ${clientException?.message}")
                    Log.d(TAG, "onFailure: ${clientException?.localizedMessage}")
                    Log.d(TAG, "onFailure: ${serviceException?.message}")
                    Log.d(TAG, "onFailure: ${serviceException?.rawMessage}")
                }

            })
        }
//        
        
    }

    fun downloadFile(key : String) {
        Log.d(TAG, "downloadFile: $key")
        val get = GetObjectRequest(Config.BUCKET_NAME, key)
        oss?.apply {
            asyncGetObject(get, object : OSSCompletedCallback<GetObjectRequest, GetObjectResult> {
                override fun onSuccess(request: GetObjectRequest?, result: GetObjectResult?) {
                    result?.apply {
                        val length = contentLength
                        if (length > 0 ) {
                            val buffer = ByteArray(length.toInt())
                            var readCount = 0
                            while (readCount < length) {
                                try {
                                    readCount += objectContent.read(buffer, readCount,
                                        (length - readCount).toInt()
                                    )
                                } catch (e : Exception) {
                                    e.printStackTrace()
                                    Log.d(TAG, "error: $e")
                                }
                            }
                            FaceServer.instance.registerByByteArray(buffer, key)
                        }
                    }
                }

                override fun onFailure(
                    request: GetObjectRequest?,
                    clientException: ClientException?,
                    serviceException: ServiceException?
                ) {
                    Log.d(TAG, "onFailure: ")
                    Log.d(TAG, "onFailure: ${serviceException?.rawMessage}")
                    Log.d(TAG, "onFailure: ${clientException?.message}")
                    Log.d(TAG, "onFailure: ${serviceException?.message}")
                }

            })
        }
    }

    fun createDir(dirPath : String) {
        val fileDir = File(dirPath)
        if (fileDir.exists()) {
            Log.d(TAG, " the directory $dirPath has already exists")
            return
        }




    }

    companion object {
        const val TAG = "OSSUploader"
        private var ossUploader : OSSUploader? = null
        private var oss : OSSClient? = null
        val instance : OSSUploader
            get() {
                if (ossUploader == null) {
                    synchronized(OSSUploader::class.java) {
                        if (ossUploader == null)
                            ossUploader = OSSUploader()
                    }
                }
                return ossUploader!!
            }

    }

}