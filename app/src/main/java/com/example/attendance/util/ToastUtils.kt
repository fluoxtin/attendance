package com.example.attendance.util

import android.widget.Toast
import com.example.attendance.App

object ToastUtils {

    fun showShortToast(message : String) {
        Toast.makeText(App.getInstance(), message, Toast.LENGTH_SHORT).show()
    }

    fun showLongToast(message: String) {
        Toast.makeText(App.getInstance(), message, Toast.LENGTH_LONG).show()
    }

}