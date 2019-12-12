package ru.skillbranch.kotlinexample

import android.text.TextUtils.replace
import java.lang.IllegalArgumentException


object UserHolder {

    private val map = mutableMapOf<String,User>()

     fun registerUser(
        fullName: String,
        email:String,
        password:String
    ): User {

        val newUser = User.makeUser(fullName,email = email,password = password)

        if(map.containsKey(newUser.login)) { throw IllegalArgumentException("A user with this email already exists")}

        return newUser.also { user -> map[user.login] = user }
//        return User.makeUser(fullName, email = email, password = password)
//            .also { user -> map[user.login] = user}
    }

     fun loginUser(login:String, password:String):String?{
        var _login = login.trim()
        if(login.startsWith("+")){
            _login = login.replace("[^+\\d]".toRegex(), "")
        }
        return map[_login]?.run{
            if(checkPassword(password))  this.userInfo
            else  null
        }
    }

     fun registerUserByPhone(fullName:String, rawPhone:String):User{

         var testPhone = rawPhone.replace("[^+\\d]".toRegex(), "")

         if(testPhone.startsWith("+")) {
             testPhone = testPhone.substring(1)
         }
         if (testPhone.length != 11) {
             throw IllegalArgumentException("Enter a valid phone starting with a + and containing 11 digits")
         }

        val newUser =  User.makeUser(fullName,null,null,rawPhone)

        if(map.containsKey(newUser.login)) { throw IllegalArgumentException("A user with this  phone already exists")}

        return newUser.also { user -> map[user.login] = user }

    }


    fun requestAccessCode(login: String) {
        val oldLogin = login.replace("[^+\\d]".toRegex(), "")
        var user:User? = null
        for (element in map ){
            if(element.key == oldLogin) user = element.value
        }
        if(user == null) {
            return
        }
        user.changePassword()
    }

    fun clearMap(){
        map.clear()
    }

}



