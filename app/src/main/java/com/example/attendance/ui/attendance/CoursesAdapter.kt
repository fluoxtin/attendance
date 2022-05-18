package com.example.attendance.ui.attendance

import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.databinding.CourseItemBinding
import com.example.attendance.model.Course

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
        fun bind(item : Course) {
            binding.courseName.text = item.cour_name
            binding.teacherName.text = item.tea_name
            binding.classTime.text = item.class_time
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
    }
}