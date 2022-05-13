package com.example.attendance.ui.report

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.databinding.AttendanceRecordItemBinding
import com.example.attendance.model.AttendanceRecord

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
        fun bind(item : AttendanceRecord) {
            binding.courseName.text = item.cour_name
            binding.recordTime.text = item.time
            binding.isAttendance.text = when(item.isAttendance) {
                true -> "出勤"
                else -> "缺勤"
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