package com.example.attendance.ui.personal

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import com.example.attendance.R
import com.example.attendance.databinding.ActivityModifyInfoBinding

class ModifyInfoActivity : AppCompatActivity() {

    lateinit var binding : ActivityModifyInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModifyInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.submit.setOnClickListener {
            // TODO:

            finish()
        }
    }
}