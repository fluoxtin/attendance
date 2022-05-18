package com.example.attendance.ui.personal

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.attendance.R
import com.example.attendance.databinding.ActivityRequestLeaveBinding

class RequestLeaveActivity : AppCompatActivity() {

    lateinit var binding : ActivityRequestLeaveBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestLeaveBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.submit.setOnClickListener {
            // TODO:  
            finish()
        }

    }
}