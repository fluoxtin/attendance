package com.example.attendance.ui.attendance

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendance.R
import com.example.attendance.databinding.FragmentAttendanceBinding


class AttendanceFragment : Fragment() {

    lateinit var adapter: CoursesAdapter
    lateinit var binding : FragmentAttendanceBinding
    private val viewModel by viewModels<AttendViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentAttendanceBinding.inflate(inflater, container, false).also {
        binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = CoursesAdapter()

        binding.courseList.adapter = adapter
        binding.courseList.layoutManager = LinearLayoutManager(requireContext())

        viewModel.currentUser.observe(viewLifecycleOwner) {
            it?.apply {
                if (role == 0) {
                    viewModel.getTeacherInfo(username)
                    viewModel.getTeacherCourse(username)
                } else if (role == 1) {
                    viewModel.getStudentInfo(username)
                    viewModel.getStudentCourse(username)
                    viewModel.start()
                    viewModel.getTaskForS(username)
                }
            }
        }

        viewModel.currentStudent.observe(viewLifecycleOwner) {
            it?.apply {
                binding.welcomeUser.text = getString(R.string.welcome_user, it.name)
            }
        }

        viewModel.currentTeacher.observe(viewLifecycleOwner)  {
            it?.apply {
                binding.welcomeUser.text = getString(R.string.welcome_user, it.name)
            }
        }

        viewModel.course.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

    }

    override fun onStop() {
        super.onStop()
        viewModel.stop()
    }

    companion object {
        const val TAG = "AttendanceFragment"

    }
}