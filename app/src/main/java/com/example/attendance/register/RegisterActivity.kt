package com.example.attendance.register

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
import androidx.lifecycle.ViewModelProvider
import com.example.attendance.ui.MainActivity
import com.example.attendance.R
import com.example.attendance.databinding.ActivityRegisterBinding
import com.example.attendance.model.Student
import com.example.attendance.model.Teacher
import com.example.attendance.util.SharedPreferencesUtils
import com.example.attendance.util.ToastUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    lateinit var binding : ActivityRegisterBinding
    private val registerViewModel by viewModels<RegisterViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val name = binding.name
        val phone = binding.phoneNum
        val email = binding.email
        val unit = binding.unit
        var sex : String = ""
        val stuClass = binding.stuClass
        val major = binding.major
        val radioGroup = binding.sexRadioGroup
        val complete = binding.complete

        radioGroup.setOnCheckedChangeListener { _, checkId ->
            sex = when(checkId) {
                R.id.male -> "male"
                R.id.female -> "female"
                else -> ""
            }

            if (sex.isNotBlank()) {
                binding.sexErrorTip.visibility = View.GONE
            }

        }

        registerViewModel.registerFormState.observe(this, Observer {

            when {
                it.nameError != null -> name.error = getString(it.nameError)
                it.phoneError != null -> phone.error = getString(it.phoneError)
                it.emailError != null -> email.error = getString(it.emailError)
                it.unitError != null -> unit.error = getString(it.unitError)
                it.stuClassError != null -> stuClass.error = getString(it.stuClassError)
                it.majorError != null -> major.error = getString(it.majorError)

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

        name.afterTextChanged {
            registerViewModel.nameDataChanged(it)
        }

        phone.afterTextChanged {
            registerViewModel.phoneDataChanged(it)
        }

        email.afterTextChanged {
            registerViewModel.emailDataChanged(it)
        }

        unit.afterTextChanged { registerViewModel.unitDataChanged(it) }

        stuClass.afterTextChanged { registerViewModel.classDataChanged(it) }

        major.afterTextChanged { registerViewModel.majorDataChanged(it) }

        binding.complete.setOnClickListener {
            if (sex.isBlank()) {
                binding.sexErrorTip.visibility = View.VISIBLE
                return@setOnClickListener
            }
            binding.waiting.visibility = View.VISIBLE
            SharedPreferencesUtils.getCurrentUser()?.apply {
                if (role == 0) {
                    val teacher = Teacher(
                        this.username,
                        name.text.toString(),
                        sex,
                        phone.text.toString(),
                        email.text.toString(),
                        unit.text.toString()
                    )
                    registerViewModel.updateTeaInfo(teacher)
                } else {
                    val student = Student(
                        this.username,
                        name.text.toString(),
                        sex,
                        phone.text.toString(),
                        email.text.toString(),
                        unit.text.toString(),
                        stuClass.text.toString(),
                        major.text.toString()
                    )
                    registerViewModel.updateStudentInfo(student)
                }
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