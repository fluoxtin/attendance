package com.example.attendance.ui.personal

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.attendance.databinding.FragmentPersonalPageBinding

// TODO: Rename parameter arguments, choose names that match

/**
 * A simple [Fragment] subclass.
 * create an instance of this fragment.
 */
class PersonalPageFragment : Fragment() {

    lateinit var binding : FragmentPersonalPageBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =  FragmentPersonalPageBinding.inflate(inflater, container, false).also {
        binding = it
    }.root


}