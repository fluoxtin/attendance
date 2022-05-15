package com.example.attendance.ui.login

/**
 *  @author fluoxtin created on 22/4/23
 *
 *
 */

data class LoginFormState(
    val usernameError : Int? = null,
    val passwordError : Int? = null,
    val isDataValid : Boolean = false
)
