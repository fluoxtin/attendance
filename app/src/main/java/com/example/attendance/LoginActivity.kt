package com.example.attendance

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.attendance.databinding.ActivityLoginBinding
import com.example.attendance.login.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel : LoginViewModel
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

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

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
                val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
                startActivity(intent)
            }
            if (loginResult.success != null) {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            }
            setResult(Activity.RESULT_OK)
            finish()
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

    private fun showLoginFailed(@StringRes errorString : Int) {
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
