package com.example.attendance.ui.personal

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.attendance.R
import com.example.attendance.databinding.ActivityFeedbackBinding

class FeedbackActivity : AppCompatActivity() {
    
    lateinit var binding : ActivityFeedbackBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.submit.setOnClickListener {
            // TODO:  
            finish()
        }
        
    }
}