package com.example.attendance.ui

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import com.example.attendance.R
import com.example.attendance.api.APIService
import com.example.attendance.api.retrofit.Results
import com.example.attendance.api.retrofit.RetrofitManager
import com.example.attendance.databinding.ActivitySplashBinding
import com.example.attendance.login.LoginActivity
import com.example.attendance.model.User
import com.example.attendance.util.SharedPreferencesUtils
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding


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

        RetrofitManager.getService(APIService::class.java)
            .isLogin()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Results<User>> {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext (t: Results<User>) {
                    t.apply {
                        when(code) {
                            200 -> {
                                data?.apply { SharedPreferencesUtils.putCurrentUser(this) }
                                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                            }
                            else -> startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                        }
                        finish()
                    }
                }

                override fun onError(e: Throwable) {
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    finish()
                    Log.e(TAG, "onError: ${e.message}", )
                }

                override fun onComplete() {
                }
            })
    }


    companion object {
        const val TAG = "SplashActivity"
    }
}