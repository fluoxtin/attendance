package com.example.attendance.ui

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.WindowManager
import com.example.attendance.R
import com.example.attendance.api.APIService
import com.example.attendance.api.retrofit.Results
import com.example.attendance.api.retrofit.RetrofitManager
import com.example.attendance.databinding.ActivitySplashBinding
import com.example.attendance.ui.login.LoginActivity
import com.example.attendance.model.User
import com.example.attendance.util.SharedPreferencesUtils
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.internal.schedulers.ExecutorScheduler
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    private val service  =  Executors.newSingleThreadScheduledExecutor()

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.getColor(R.color.white)
        var intent = Intent(this, LoginActivity::class.java)

        RetrofitManager.getService(APIService::class.java)
            .isLogin()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Results<User>> {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext (t: Results<User>) {
                    t.apply {
                        intent = when(code) {
                            200 -> {
                                data?.apply { SharedPreferencesUtils.putCurrentUser(this) }
                                Intent(this@SplashActivity, MainActivity::class.java)
                            }
                            else -> Intent(this@SplashActivity, LoginActivity::class.java)
                        }
                    }
                }

                override fun onError(e: Throwable) {
                    intent = Intent(this@SplashActivity, LoginActivity::class.java)
                    Log.e(TAG, "onError: ${e.message}", )
                }

                override fun onComplete() {
                }
            })

        service.schedule({
            startActivity(intent)
            finish()
        }, 2, TimeUnit.SECONDS)

    }

    override fun onDestroy() {
        super.onDestroy()
        if (!service.isShutdown)
            service.shutdown()
    }

    companion object {
        const val TAG = "SplashActivity"
    }
}