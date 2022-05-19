package com.example.attendance.ui.report

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendance.databinding.FragmentReportBinding
import com.example.attendance.util.SharedPreferencesUtils
import com.example.attendance.util.ToastUtils

/**
 * A simple [Fragment] subclass.
 * create an instance of this fragment.
 */
class ReportFragment : Fragment() {

    lateinit var binding: FragmentReportBinding
    private var stuRecordAdapter : StuAttendRecordAdapter? = null
    private var courseRecordAdapter : CourRecordAdapter? = null

    private val viewModel by viewModels<ReportViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentReportBinding.inflate(inflater, container, false).also {
        binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when(SharedPreferencesUtils.getCurrentUserRole()) {
            0 -> initViewForT()
            1 -> initViewForS()
            else -> ToastUtils.showShortToast("undefined role")
        }

    }

    private fun initViewForT() {
        courseRecordAdapter = CourRecordAdapter()
        binding.totalTaskInfo.visibility = View.GONE
        binding.attendanceInfo.visibility = View.GONE
        binding.absenceInfo.visibility = View.GONE

        binding.records.adapter = courseRecordAdapter
        binding.records.layoutManager = LinearLayoutManager(requireContext())

        viewModel.courseRecords.observe(viewLifecycleOwner) {
            courseRecordAdapter?.submitList(it)
        }

    }

    private fun initViewForS() {
        stuRecordAdapter = StuAttendRecordAdapter()
        binding.totalTaskInfo.visibility = View.VISIBLE
        binding.attendanceInfo.visibility = View.VISIBLE
        binding.absenceInfo.visibility = View.VISIBLE

        binding.records.adapter = stuRecordAdapter
        binding.records.layoutManager = LinearLayoutManager(requireContext())

        viewModel.stuAttendRecords.observe(viewLifecycleOwner) {
            stuRecordAdapter?.submitList(it)

            binding.totalTaskNum.text = it.size.toString()
            var attendNum = 0
            for (record in it) {
                if (record.isAttendance == 1)
                    attendNum++
            }
            binding.totalAttendanceNumber.text = attendNum.toString()
            binding.totalAbsenceNumber.text = (it.size - attendNum).toString()
        }
    }

}