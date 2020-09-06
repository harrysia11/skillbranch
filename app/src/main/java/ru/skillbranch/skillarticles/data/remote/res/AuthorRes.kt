package ru.skillbranch.skillarticles.data.remote.res

import com.squareup.moshi.Json

class AuthorRes (
    @Json(name = "name")
    val name: String,
    @Json(name = "id")
    val id: String,
    @Json(name = "avatar")
    val avatar: String
)