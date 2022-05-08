package com.example.attendance.login.data



/**
 * @author: fluoxtin created on 2022/4/23
 */
class LoginRepository(val dataSource: LoginDataSource) {

    var user: LoggedInUser? = null
        private set

    val isLoggedIn : Boolean
        get() = user!= null

    init {
        user = null
    }

    fun logout() {
        user = null
        dataSource.logout()
    }

    fun login(username: String, password : String, role : Int): Result<LoggedInUser> {
        val result = dataSource.login(username, password, role)

        if (result is Result.Success) {
            setLoggedInUser(result.data)
        }

        return result
    }

    private fun setLoggedInUser(loggedInUser: LoggedInUser) {
        this.user = loggedInUser
    }

}