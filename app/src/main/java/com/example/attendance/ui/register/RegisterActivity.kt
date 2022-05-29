package com.example.attendance.ui.register

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.example.attendance.ui.MainActivity
import com.example.attendance.R
import com.example.attendance.databinding.ActivityRegisterBinding
import com.example.attendance.model.Student
import com.example.attendance.model.Teacher
import com.example.attendance.util.SharedPreferencesUtils
import com.example.attendance.util.ToastUtils
import dagger.hilt.android.AndroidEntryPoint

class RegisterActivity : AppCompatActivity() {

    lateinit var binding : ActivityRegisterBinding
    private val registerViewModel by viewModels<RegisterViewModel>()

    private var sex = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        when(SharedPreferencesUtils.getCurrentUserRole()) {
            0 -> initViewForT()
            1 -> initVewForS()
            else -> ToastUtils.showShortToast("undefined role")
        }

        binding.sexRadioGroup.setOnCheckedChangeListener { _, checkId ->
            sex = when(checkId) {
                R.id.male -> "男"
                R.id.female -> "女"
                else -> ""
            }

            if (sex.isNotBlank()) {
                binding.sexErrorTip.visibility = View.GONE
            }

        }

        registerViewModel.registerFormState.observe(this, Observer {

            when {
                it.nameError != null -> binding.name.error = getString(it.nameError)
                it.phoneError != null -> binding.phoneNum.error = getString(it.phoneError)
                it.emailError != null -> binding.email.error = getString(it.emailError)
                it.unitError != null -> binding.unit.error = getString(it.unitError)
                it.stuClassError != null -> binding.stuClass.error = getString(it.stuClassError)
                it.majorError != null -> binding.major.error = getString(it.majorError)

                else -> binding.complete.isEnabled = true
            }


        })

        registerViewModel.registerResult.observe(this, Observer {

            binding.waiting.visibility = View.GONE

            if (it.success != null) {
                startActivity(Intent(this, MainActivity::class.java))
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                ToastUtils.showLongToast("Update info error with error code : ${it.error}")
            }

        })

        binding.name.afterTextChanged {
            registerViewModel.nameDataChanged(it)
        }

        binding.phoneNum.afterTextChanged {
            registerViewModel.phoneDataChanged(it)
        }

        binding.email.afterTextChanged {
            registerViewModel.emailDataChanged(it)
        }

        binding.unit.afterTextChanged { registerViewModel.unitDataChanged(it) }


    }

    private fun initViewForT() {

        binding.stuClass.visibility = View.GONE
        binding.major.visibility = View.GONE

        binding.complete.setOnClickListener {
            SharedPreferencesUtils.getCurrentUser()?.apply {
                val teacher = Teacher(
                    username,
                    binding.name.text.toString(),
                    sex,
                    binding.phoneNum.text.toString(),
                    binding.email.text.toString(),
                    binding.unit.text.toString()
                )
                registerViewModel.updateTeaInfo(teacher)
            }
        }
    }

    private fun initVewForS() {

        binding.stuClass.visibility = View.VISIBLE
        binding.major.visibility = View.VISIBLE

        binding.stuClass.afterTextChanged {
            registerViewModel.classDataChanged(it)
        }

        binding.major.afterTextChanged {
            registerViewModel.majorDataChanged(it)
        }

        binding.complete.setOnClickListener {
            SharedPreferencesUtils.getCurrentUser()?.apply {
                val student = Student(
                    username,
                    binding.name.text.toString(),
                    sex,
                    binding.phoneNum.text.toString(),
                    binding.email.text.toString(),
                    binding.unit.text.toString(),
                    binding.stuClass.text.toString(),
                    binding.major.text.toString(),
                    null
                )
                registerViewModel.updateStudentInfo(student)
            }
        }
    }

    private fun EditText.afterTextChanged(afterTextChanged : (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                afterTextChanged.invoke(s.toString())
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })

    }
}