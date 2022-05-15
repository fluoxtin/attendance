package com.example.attendance.ui.personal

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.attendance.databinding.FragmentPersonalPageBinding
import com.example.attendance.ui.arcface.RecognizeFaceActivity
import com.example.attendance.util.ToastUtils


class PersonalPageFragment : Fragment() {

    lateinit var binding : FragmentPersonalPageBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =  FragmentPersonalPageBinding.inflate(inflater, container, false).also {
        binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.uploadFaceInfo.setOnClickListener {

            ToastUtils.showLongToast("ahahah")
            val intent = Intent(requireContext(), RecognizeFaceActivity::class.java)
            requireContext().startActivity(intent)
        }
    }


}