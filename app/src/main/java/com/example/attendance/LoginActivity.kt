package com.example.attendance

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.example.attendance.databinding.ActivityLoginBinding
import com.example.attendance.login.LoggedInUserView
import com.example.attendance.login.LoginViewModel
import com.example.attendance.login.LoginViewModelFactory

class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel : LoginViewModel
    private lateinit var binding: ActivityLoginBinding
    private var role : Int = 0

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
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
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
                else -> 1
            }
        }

        login.setOnClickListener {
            loginViewModel.login(username.text.toString(), password.text.toString(), role)
        }

    }

    private fun updateUiWithUser(model : LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.username

    }

    private fun showLoginFailed(@StringRes errorString : Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }

    fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
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
