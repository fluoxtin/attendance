package com.example.attendance.ui.login

/**
 * @author: fluoxtin created on 2022/4/23
 */

data class LoginResult(
    val success : String? = null,
    val needRegister : Boolean? = null,
    val error : String? = null
)
