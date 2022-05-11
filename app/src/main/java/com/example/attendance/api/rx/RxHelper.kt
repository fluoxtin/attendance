package com.example.attendance.api.rx

import com.example.attendance.api.retrofit.Results
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.Observer
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*

object RxHelper {

//    fun <T> resultTransformer() : ObservableTransformer<Results<T>, T> {
//        return object : ObservableTransformer<Results<T>, T> {
//            override fun apply(upstream: Observable<Results<T>>): ObservableSource<T> {
//                return upstream.subscribeOn(Schedulers.io())
//                    .observeOn(AndroidSchedulers.mainThread())
//
//            }
//
//        }
//    }

}