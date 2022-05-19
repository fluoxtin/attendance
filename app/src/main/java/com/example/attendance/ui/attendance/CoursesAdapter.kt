package com.example.attendance.ui.attendance

import android.annotation.SuppressLint
import android.icu.util.LocaleData
import android.os.Build
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.databinding.CourseItemBinding
import com.example.attendance.model.Course
import com.example.attendance.util.SharedPreferencesUtils
import com.example.attendance.util.ToastUtils
import java.sql.Time
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class CoursesAdapter : ListAdapter<Course, CoursesAdapter.CourseViewHolder>(CALLBACK) {

    var course : Course? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        return CourseViewHolder(
            CourseItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        getItem(position).let {
            holder.bind(it)
        }

        holder.binding.root.setOnLongClickListener {
            course = getItem(position)

            false
        }

    }

    class CourseViewHolder(
        val binding : CourseItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item : Course) {
            binding.courseName.text = item.cour_name
            binding.teacherName.text = item.tea_name
            binding.classTime.text = "${item.start} - ${item.end}"

//            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
//            val date = LocalDate.parse(item.start)
            val startT = item.start.split(":")
            val endT = item.end.split(":")
            if (isCurrentInTimeScope(
                    startT[0].toInt(),
                    startT[1].toInt(),
                    endT[0].toInt(),
                    endT[1].toInt()
            ))
                binding.isClassTime.visibility = View.VISIBLE
            else
                binding.isClassTime.visibility = View.GONE

            when(SharedPreferencesUtils.getCurrentUserRole()) {
                0 -> binding.teacherName.visibility = View.GONE
                1 -> binding.teacherName.visibility = View.VISIBLE
                else -> ToastUtils.showShortToast("undefined role")
            }
        }
    }



    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<Course>() {
            override fun areItemsTheSame(oldItem: Course, newItem: Course): Boolean {
                return oldItem.cour_id == newItem.cour_id
            }

            override fun areContentsTheSame(oldItem: Course, newItem: Course): Boolean {
                return oldItem == newItem
            }
        }

        fun isCurrentInTimeScope(beginH : Int, beginM : Int, endH : Int, endM : Int)
            : Boolean {
            val cur = System.currentTimeMillis()
            val now = Time(cur)
            val start = Time(cur)
            val end = Time(cur)
            start.hours = beginH
            start.minutes = beginM
            end.hours = endH
            end.minutes = endM
            return now.before(end) && now.after(start)
        }
    }
}