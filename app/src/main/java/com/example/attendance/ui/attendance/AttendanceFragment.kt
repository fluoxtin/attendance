package com.example.attendance.ui.attendance

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendance.R
import com.example.attendance.databinding.FragmentAttendanceBinding
import com.example.attendance.model.AttendTask
import com.example.attendance.model.Course
import com.example.attendance.ui.arcface.RecognizeFaceActivity
import com.example.attendance.util.SharedPreferencesUtils
import com.example.attendance.util.ToastUtils
import java.util.*


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

        when(SharedPreferencesUtils.getCurrentUserRole()) {
            0 -> initViewForT()
            1 -> initViewForS()
            else -> ToastUtils.showShortToast("undefined role")
        }

        viewModel.course.observe(viewLifecycleOwner) {
            binding.courseTips.text = getString(R.string.course_tip, it.size)
            adapter.submitList(it)
        }

        viewModel.initLocationClient(requireContext().applicationContext)
    }

    private fun initViewForT() {

        viewModel.currentTeacher.observe(viewLifecycleOwner)  {
            it?.apply {
                binding.welcomeUser.text = getString(R.string.welcome_user, it.tea_name)
            }
        }

        registerForContextMenu(binding.courseList)

    }

    private fun initViewForS() {

        viewModel.currentStudent.observe(viewLifecycleOwner) {
            it?.apply {
                binding.welcomeUser.text = getString(R.string.welcome_user, it.name)
            }
        }

        viewModel.attendTask.observe(viewLifecycleOwner) {
            it?.apply {
                binding.attendanceTaskTip.visibility = View.VISIBLE
                binding.location.text = getString(R.string.destination_location,
                    "${location.latitude},${location.longitude}")
            }
            if (it == null) {
                binding.attendanceTaskTip.visibility = View.GONE
            }
        }

        viewModel.currLocation.observe(viewLifecycleOwner) {
            it?.apply {
                binding.currLocation.text = getString(com.example.attendance.R.string.curr_location, "${it.latitude},${it.longitude}")
            }
        }

        binding.signInBtn.setOnClickListener {
            requireContext().startActivity(
                Intent(requireContext(), RecognizeFaceActivity::class.java)
            )
        }

    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        requireActivity().menuInflater.inflate(R.menu.post_task_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (isVisible) {
            when(item.itemId) {
                R.id.post_task -> {
                    val course = adapter.course
                    if (course == null) {
                        ToastUtils.showShortToast("Error : course is null")
                        return true
                    }
                    viewModel.postTask(course)
                }
                else -> ToastUtils.showShortToast("error")
            }
            return true
        }
        return false
    }

    override fun onStop() {
        super.onStop()
        viewModel.stop()
    }

    companion object {
        const val TAG = "AttendanceFragment"

    }


}