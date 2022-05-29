package com.example.attendance.ui.personal

import android.app.DatePickerDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import com.example.attendance.R
import com.example.attendance.databinding.ActivityRequestLeaveBinding
import com.example.dateandtimepicker.SingleDateAndTimePicker
import com.example.dateandtimepicker.dialog.SingleDateAndTimePickerDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

class RequestLeaveActivity : AppCompatActivity() {

    lateinit var binding : ActivityRequestLeaveBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestLeaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val singleBuilder = SingleDateAndTimePickerDialog.Builder(this)
            .setTimeZone(TimeZone.getDefault())
            .bottomSheet()
            .curved()
//            .defaultDate(Date())
//            .titleTextColor()
//            .backgroundColor()
//            .mainColor()
            .displayMinutes(true)
            .displayHours(true)
            .displayDays(true)
//            .displayMonth()
//            .displayYears()
            .displayListener(object : SingleDateAndTimePickerDialog.DisplayListener {
                override fun onDisplayed(picker: SingleDateAndTimePicker?) {
                    Log.d(TAG, "onDisplayed: ")
                }

                override fun onClosed(picker: SingleDateAndTimePicker?) {
                    Log.d(TAG, "onClosed: ")
                }

            })
            .title("Simple Time")
            .listener {
                binding.startTimeInfo.text = SimpleDateFormat("yyyy-MM-dd HH:mm").format(it)
            }


        binding.start.setOnClickListener {
            Log.d(TAG, "onCreate: start clicked")
            singleBuilder.display()
        }




        binding.submit.setOnClickListener {
            // TODO:  
            finish()
        }

    }

    companion object {
        const val TAG = "RequestLeaveActivity"
    }
}