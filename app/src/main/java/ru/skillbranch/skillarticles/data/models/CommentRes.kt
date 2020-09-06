package ru.skillbranch.skillarticles.data.models

import com.squareup.moshi.Json
import java.util.*

data class CommentRes(
    val id: String,
    val articleId: String ,
    @Json(name = "author")
    val user : User,
    @Json(name = "message")
    val body: String,
    val date: Date,
    val slug:String,
    val answerTo:String? = null
) {

}