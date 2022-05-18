package com.example.attendance.ui.personal

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.attendance.databinding.FragmentPersonalPageBinding
import com.example.attendance.faceserver.FaceServer
import com.example.attendance.model.Course
import com.example.attendance.ui.attendance.AttendViewModel
import com.example.attendance.ui.login.LoginActivity
import com.example.attendance.util.SharedPreferencesUtils
import com.example.attendance.util.ToastUtils


class PersonalPageFragment : Fragment() {

    lateinit var binding : FragmentPersonalPageBinding
    private val viewModel by viewModels<AttendViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =  FragmentPersonalPageBinding.inflate(inflater, container, false).also {
        binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when(SharedPreferencesUtils.getCurrentUserRole()) {
            0 -> initViewForT()
            1 -> initViewForS()
            else -> ToastUtils.showShortToast("undefined role")
        }

        binding.modifyPassword.setOnClickListener {

        }

        binding.feedback.setOnClickListener {

        }

        binding.logout.setOnClickListener {
            SharedPreferencesUtils.removeUser()
            SharedPreferencesUtils.removeToken()
            requireContext().startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

    }

    private fun initViewForT() {

        binding.uploadFaceInfo.visibility = View.GONE
        binding.leave.visibility = View.GONE

        viewModel.currentTeacher.observe(viewLifecycleOwner) {
            binding.username.text = it.tea_name
            binding.unit.text = it.unit
        }

    }

    private fun initViewForS() {

        binding.uploadFaceInfo.setOnClickListener {

            FaceServer.instance.init(requireContext())

            val intent = Intent(Intent.ACTION_PICK)
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/**")
            requireActivity().startActivityForResult(intent, ACTION_PICK_IMAGE)
        }

        binding.leave.setOnClickListener {
            requireContext().startActivity(Intent(requireContext(), RequestLeaveActivity::class.java))
        }

        viewModel.currentStudent.observe(viewLifecycleOwner) {
            binding.username.text = it.name
            binding.unit.text = it.unit
            binding.studentClass.text = it.stu_class
        }
    }

    companion object {
        const val ACTION_PICK_IMAGE = 0X202
    }
}