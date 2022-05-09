package com.example.attendance

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.example.attendance.databinding.ActivitySplashBinding
import com.example.attendance.login.APIService
import com.example.attendance.retrofit.RetrofitManager
import com.example.attendance.util.SharedPreferencesUtils

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

        val user = SharedPreferencesUtils.getCurrentUser()
        val intent = when {
            SharedPreferencesUtils.hasCurrentUserInfo() != null -> {
                Intent(this, MainActivity::class.java)
            }
            user != null -> {
                Intent(this, RegisterActivity::class.java)
            }
            else -> {
                Intent(this, LoginActivity::class.java)
            }
        }

        Handler().postDelayed({

            startActivity(intent)
            finish()
        }, 3000)

    }




    companion object {

    }
}