package com.example.attendance.ui.report

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.App
import com.example.attendance.R
import com.example.attendance.databinding.AttendanceRecordItemBinding
import com.example.attendance.model.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.*

class StuAttendRecordAdapter :
    ListAdapter<AttendanceRecord, StuAttendRecordAdapter.RecordViewHolder>(CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        return RecordViewHolder(
            AttendanceRecordItemBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }


    class RecordViewHolder(val binding: AttendanceRecordItemBinding)
        : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SimpleDateFormat")
        fun bind(item : AttendanceRecord) {
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(item.sign_in_time))
            binding.courseName.text = item.cour_name
            binding.recordTime.text = time
            binding.isAttendance.text = when(item.isAttendance) {
                1 -> {
                    binding.isAttendance.setTextColor(App.getInstance().getColor(R.color.attendance))
                    "出勤"
                }
                else -> {
                    binding.isAttendance.setTextColor(App.getInstance().getColor(R.color.absence))
                    "缺勤"
                }
            }
        }
    }


    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<AttendanceRecord>() {
            override fun areItemsTheSame(oldItem: AttendanceRecord, newItem: AttendanceRecord): Boolean {
                return oldItem.attend_id == newItem.attend_id
            }

            override fun areContentsTheSame(oldItem: AttendanceRecord, newItem: AttendanceRecord): Boolean {
                return oldItem == newItem
            }

        }
    }
}