package com.example.attendance.ui.report

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.App
import com.example.attendance.R
import com.example.attendance.databinding.StuRecordItemBinding
import com.example.attendance.model.StudentRecord
import java.text.SimpleDateFormat
import java.util.*

class StudentRecordAdapter :
    ListAdapter<StudentRecord, StudentRecordAdapter.RecordViewHolder>(CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        return RecordViewHolder(
            StuRecordItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecordViewHolder(val binding: StuRecordItemBinding)
        : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SimpleDateFormat")
        fun bind(item : StudentRecord) {
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(item.sign_in_time))
            binding.name.text = item.stu_name
            binding.recordTime.text = time
            binding.isAttendance.text = when(item.attendance) {
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
        private val CALLBACK = object : DiffUtil.ItemCallback<StudentRecord>() {
            override fun areItemsTheSame(oldItem: StudentRecord, newItem: StudentRecord): Boolean {
                return oldItem.stu_id == newItem.stu_id
            }

            override fun areContentsTheSame(oldItem: StudentRecord, newItem: StudentRecord): Boolean {
                return oldItem == newItem
            }

        }
    }
}