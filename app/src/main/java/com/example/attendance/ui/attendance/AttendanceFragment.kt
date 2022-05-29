package com.example.attendance.ui.attendance

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
                when(sex) {
                    "男" -> binding.userPhoto.setImageResource(R.drawable.male_image)
                    "女" -> binding.userPhoto.setImageResource(R.drawable.female_image)
                    else -> binding.userPhoto.setImageResource(R.drawable.personal_icon)
                }
            }
        }

        viewModel.task.observe(viewLifecycleOwner) {
            it?.apply {
                binding.postTaskInfoCv.visibility = View.VISIBLE
                binding.postTaskInfo.text = getString(R.string.current_task_info, cour_id)
                viewModel.startCountdown(deadline)
            }
            if (it == null)
                binding.postTaskInfoCv.visibility = View.GONE
        }

        viewModel.countdown.observe(viewLifecycleOwner) {
            it?.apply {
                if (it < 30)
                    binding.countdown.setTextColor(Color.RED)
                else binding.countdown.setTextColor(Color.BLACK)
                binding.countdown.text = it.toString()
            }
        }

        registerForContextMenu(binding.courseList)

    }

    private fun initViewForS() {

        viewModel.currentStudent.observe(viewLifecycleOwner) {
            it?.apply {
                binding.welcomeUser.text = getString(R.string.welcome_user, it.name)

                when(sex) {
                    "男" -> binding.userPhoto.setImageResource(R.drawable.male_image)
                    "女" -> binding.userPhoto.setImageResource(R.drawable.female_image)
                    else -> binding.userPhoto.setImageResource(R.drawable.personal_icon)
                }
                face_url?.apply {
                    viewModel.loadFace(this)
                }
            }
        }

        viewModel.attendTask.observe(viewLifecycleOwner) {
            it?.apply {
                binding.attendanceTaskTip.visibility = View.VISIBLE
                binding.location.text = getString(R.string.destination_location,
                    "${location.latitude},${location.longitude}")
                viewModel.startCountdown(deadline)
                viewModel.canSignIn(location)
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

        viewModel.countdown.observe(viewLifecycleOwner) {
            it?.apply {
                binding.signInBtn.text = getString(R.string.sign_in, this)
            }
            if (it == null) {
                Log.d(TAG, "initViewForS: countdown over")
            }
        }

        viewModel.canSignIn.observe(viewLifecycleOwner) {

            binding.signInBtn.isEnabled = it
            if (!it)
                binding.errorLocation.visibility = View.VISIBLE
            else
                binding.errorLocation.visibility = View.GONE
        }

        binding.signInBtn.setOnClickListener {
            requireActivity().startActivityFromFragment(this,
                Intent(requireContext(), RecognizeFaceActivity::class.java),
                RECOGNIZE_REQUEST_CODE
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == -1) {
            if (requestCode == RECOGNIZE_REQUEST_CODE) {
                data?.apply {
                    when(getBooleanExtra("recognized", false)) {
                        true -> {
                            ToastUtils.showShortToast("recognized successfully")
                            viewModel.attendTask.value?.let {
                                viewModel.postAttendanceRecord(it, 1)
                            }
                        }
                        else -> ToastUtils.showShortToast("recognized failed")
                    }
                }
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStop() {
        super.onStop()
        viewModel.stop()
    }

    companion object {
        const val TAG = "AttendanceFragment"
        const val RECOGNIZE_REQUEST_CODE = 0x1011

    }


}