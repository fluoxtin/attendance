package com.example.attendance.ui.report

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.databinding.AttendanceRecordItemBinding
import com.example.attendance.model.CourseAttendanceRecord
import java.text.SimpleDateFormat
import java.util.*

class CourRecordAdapter :
    ListAdapter<CourseAttendanceRecord, CourRecordAdapter.CourseRecordViewHolder>(CALLBACK) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseRecordViewHolder {
        return CourseRecordViewHolder(
            AttendanceRecordItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: CourseRecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CourseRecordViewHolder(val binding: AttendanceRecordItemBinding)
        : RecyclerView.ViewHolder(binding.root) {
            @SuppressLint("SimpleDateFormat")
            fun bind(item : CourseAttendanceRecord) {
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(item.time))
                binding.courseName.text = item.cour_name
                binding.recordTime.text = time
                binding.attendNum.text = item.actual.toString()
                binding.absenceNum.text = (item.total - item.actual).toString()
                binding.isAttendance.visibility = View.GONE
            }

    }

    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<CourseAttendanceRecord>() {
            override fun areItemsTheSame(oldItem: CourseAttendanceRecord, newItem: CourseAttendanceRecord): Boolean {
                return oldItem.attend_id == newItem.attend_id
            }

            override fun areContentsTheSame(oldItem: CourseAttendanceRecord, newItem: CourseAttendanceRecord): Boolean {
                return oldItem == newItem
            }

        }
    }

}