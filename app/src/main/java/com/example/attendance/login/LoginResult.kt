package com.example.attendance.login

/**
 * @author: fluoxtin created on 2022/4/23
 */
data class LoginResult(
    val success : LoggedInUserView? = null,
    val error : Int? = null
)
