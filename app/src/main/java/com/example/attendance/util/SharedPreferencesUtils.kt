package com.example.attendance.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.attendance.App
import com.example.attendance.model.Student
import com.example.attendance.model.Teacher
import com.example.attendance.model.User
import java.io.*

object SharedPreferencesUtils {

    private const val TOKEN_KEY = "token_key"
    private const val CURRENT_USER_KEY = "currentUser"
    private const val CURRENT_TEACHER_KEY = "currentTeacher"
    private const val CURRENT_STUDENT_KEY = "currentStudent"

    private var sharedPreferences : SharedPreferences? = null

    private fun getSharedPreferences() : SharedPreferences? {
        if (sharedPreferences == null && App.getInstance() != null) {
            sharedPreferences = App.getInstance()?.getSharedPreferences("SpUtil", Context.MODE_PRIVATE)
        }
        return sharedPreferences
    }

    private fun putString(key : String, value : String) {

        val editor = getSharedPreferences()?.edit()

        editor?.putString(key, value)
        editor?.apply()
    }

    private fun getString(key : String) : String? {
        return getSharedPreferences()?.getString(key, "")
    }

    fun remove(key : String) {
        getSharedPreferences()?.edit()?.apply{
            remove(key)
            apply()
        }
    }

    private fun putObject(key: String, value : Any) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val out: ObjectOutputStream?
        try {
            out = ObjectOutputStream(byteArrayOutputStream)
            out.writeObject(value)
            val objectValue = String(Base64.encode(byteArrayOutputStream.toByteArray(), Base64.DEFAULT))
            putString(key, objectValue)
        } catch (e : IOException) {
            e.printStackTrace()
        } finally {
            byteArrayOutputStream.close()
        }
    }

    private fun getObject(key: String) : Any? {
        val sp = getSharedPreferences()
        sp?.apply {
            if (sp.contains(key)) {
                val objectValue = sp.getString(key, null)
                val buffer = Base64.decode(objectValue, Base64.DEFAULT);
                //通过读取字节流，创建字节流输入流，写入对象并作强制转换
                val bis = ByteArrayInputStream(buffer)
                var ois: ObjectInputStream? = null
                try {
                    ois = ObjectInputStream(bis)
                    return ois.readObject()
                } catch (e: StreamCorruptedException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                } finally {
                    try {
                        bis.close()
                        ois?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }

            }
        }
        return null
    }

    fun putToken(token : String) {
        putString(TOKEN_KEY, token)
    }

    fun getToken() = getString(TOKEN_KEY) ?: ""


    fun putCurrentUser(user : User) {
        putObject(CURRENT_USER_KEY, user)
    }

    fun getCurrentUser() : User? {
        return if (getObject(CURRENT_USER_KEY) == null)
            null
        else
            getObject(CURRENT_USER_KEY) as User
    }

//    fun putCurrentStudent(student: Student) {
//        putObject(CURRENT_STUDENT_KEY, student)
//    }
//
//    fun getCurrentStudent() : Student? {
//        return if (getObject(CURRENT_STUDENT_KEY) == null)
//            null
//        else getObject(CURRENT_STUDENT_KEY) as Student
//    }
//
//    fun putCurrentTeacher(teacher : Teacher) {
//        putObject(CURRENT_TEACHER_KEY, teacher)
//    }
//
//    fun getCurrentTeacher() : Teacher? {
//        return if (getObject(CURRENT_TEACHER_KEY) == null)
//            null
//        else getObject(CURRENT_TEACHER_KEY) as Teacher
//    }

//    fun hasCurrentUserInfo() : Any? {
//        return when {
//            getCurrentTeacher()!= null -> getCurrentTeacher()
//            getCurrentStudent() != null -> getCurrentStudent()
//            else -> null
//        }
//    }

    fun getCurrentUserRole() : Int {
        getCurrentUser()?.apply {
            return role
        }

        return -1
    }

    fun removeUser() {
        remove(CURRENT_USER_KEY)
    }

    fun removeToken() {
        remove(TOKEN_KEY)
    }
}