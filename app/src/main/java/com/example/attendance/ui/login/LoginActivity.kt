package com.example.attendance.ui.login

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.example.attendance.ui.MainActivity
import com.example.attendance.R
import com.example.attendance.api.retrofit.RetrofitManager
import com.example.attendance.ui.register.RegisterActivity
import com.example.attendance.databinding.ActivityLoginBinding
import com.example.attendance.util.SharedPreferencesUtils

class LoginActivity : AppCompatActivity() {

    private val loginViewModel by viewModels<LoginViewModel>()
    private lateinit var binding: ActivityLoginBinding
    private var role : Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        actionBar?.hide()

        val username = binding.username
        val password = binding.password
        val login = binding.login
        val loading = binding.loading
        val radioGroup = binding.radioGroup

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if  (it.needRegister != null) {

                AlertDialog.Builder(this@LoginActivity)
                    .setTitle("注册提醒")
                    .setMessage("当前用户${binding.username.text}未注册，是否要注册并完善相关信息")
                    .setPositiveButton("注册") { dialog, which ->
                        val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
                        startActivity(intent)
                        dialog.dismiss()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    .setNegativeButton("取消") { dialog, which ->
                        dialog.cancel()
                        loginViewModel.delete()
                        binding.username.text = null
                        binding.password.text = null
                        RetrofitManager.refresh()
                    }.show()
            }

            if (loginResult.success != null) {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                setResult(Activity.RESULT_OK)
                finish()
            }

        })


        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }
        }

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            role = when (checkedId) {
                R.id.teacher_role -> 0
                R.id.student_role -> 1
                else -> -1
            }
            if (role >= 0)
                binding.roleErrorTip.visibility = View.GONE
        }

        login.setOnClickListener {
            if (role < 0) {
                binding.roleErrorTip.visibility = View.VISIBLE
                return@setOnClickListener
            }
            loading.visibility = View.VISIBLE
            loginViewModel.login(username.text.toString(), password.text.toString(), role)
        }
    }

    private fun showLoginFailed( errorString : String) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }

    private fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                afterTextChanged.invoke(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
    }
}
