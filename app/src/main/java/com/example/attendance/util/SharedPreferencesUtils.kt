package com.example.attendance.util

import android.content.Context
import android.content.SharedPreferences
import java.util.*

class SharedPreferencesUtils {

    private var sharedPreferences : SharedPreferences? = null

    private fun getSharedPreferences(context: Context) : SharedPreferences? {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences("SpUtil", Context.MODE_PRIVATE)
        }
        return sharedPreferences
    }

    fun putString(context: Context, key : String, value : String) {
        val editor = getSharedPreferences(context)?.edit()

        editor?.putString(key, value)
        editor?.apply()
    }

    fun getString(context: Context, key : String) : String? {
        return getSharedPreferences(context)?.getString(key, "")
    }

}