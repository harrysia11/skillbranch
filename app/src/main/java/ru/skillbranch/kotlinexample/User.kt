package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    private val firstName:String,
    private val lastName:String?,
    email:String? = null,
    rawPhone:String? = null,
    meta:Map<String,Any>? = null
) {
    val userInfo: String

    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it -> it.first().toUpperCase() }
            .joinToString(" ")

    private var phone: String? = null
        set(value) {
            field = value?.replace("[^+\\d]".toRegex(), "")
        }
   //     get() = phone;

    private var _login: String? = null

     var login: String
        get() = _login!!
        set(value) {
            _login = value.toLowerCase()
        }

    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode:String? = null


    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        password: String?
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary email constructor")
        passwordHash = encypt(password ?:"")
    }


    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String?
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("Secondary constructor")
        val code:String = generateAccessCode()
        passwordHash = encypt(code)
        accessCode = code
        sendAccessCodeToUser(rawPhone,code)
    }


    init {

        println("First init block")

        check(!firstName.isBlank()){ "First name must not be blank"}
        check(!email.isNullOrBlank() || !rawPhone.isNullOrBlank()){"Email or phone must be not null"}

        phone = rawPhone
        login = email ?: phone!!

        userInfo = """
        firstName: $firstName
        lastName: $lastName
        login: $login
        fullName: $fullName
        initials: $initials
        email: $email
        phone: $phone
        meta: $meta
        """.trimIndent()
    }

    private fun generateAccessCode(): String {
        val possibleChar = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghiklmnopqrstuvwxyz0123456789"

        return StringBuilder().apply {
            repeat(6){
                (possibleChar.indices).random().also{index ->
                    append(possibleChar[index])
                }
            }
        }.toString()

    }

    private val salt:String by lazy{
        ByteArray(16).also {
            SecureRandom().nextBytes(it)}.toString()
    }

    private fun encypt(password: String): String {
        return salt.plus(password).md5()
    }

    private fun String.md5() : String{
        val md: MessageDigest = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())
        val hexString = BigInteger(1,digest).toString(16)
        return hexString.padStart(32,'0')
    }


    private fun sendAccessCodeToUser(phone: String?, code: String) {
        println(".....sending access code: $code on $phone")
    }

    fun checkPassword(password: String): Boolean = encypt(password) == passwordHash

    fun changePassword(oldPassword:String, newPassword: String){
        if(checkPassword(oldPassword)){
            passwordHash = encypt(newPassword)
        }else{
            throw IllegalArgumentException("The entered password does not match current password")
        }

    }

    companion object {

        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ): User {

            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, rawPhone = phone)
                !email.isNullOrBlank() -> User(firstName, lastName, email = email, password = password)
                else -> throw  IllegalArgumentException("Email or phone must be not null or blank")
            }

        }

        private fun String.fullNameToPair(): Pair<String, String?> {
            return this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when (size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException("FullName must contain only first name and last name, but result is ${this@fullNameToPair}")
                    }
                }
        }
    }



}