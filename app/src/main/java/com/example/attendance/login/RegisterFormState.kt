package com.example.attendance.login

data class RegisterFormState(
    val nameError : Int? = null,
    val phoneError : Int? = null,
    val emailError : Int? = null,
    val unitError : Int? = null,
    val stuClassError : Int? = null,
    val majorError : Int? = null,
)
