package ru.skillbranch.skillarticles.data.remote.res

import ru.skillbranch.skillarticles.data.models.CommentRes

data class MessageRes(
    val message: CommentRes,
    val messageCount: Int
)