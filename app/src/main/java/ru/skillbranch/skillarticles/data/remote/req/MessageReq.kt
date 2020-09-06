package ru.skillbranch.skillarticles.data.remote.req

data class MessageReq(
    private val message: String,
    private val answerTo: String? = null
    )