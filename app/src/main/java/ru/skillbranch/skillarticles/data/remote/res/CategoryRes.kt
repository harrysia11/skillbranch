package ru.skillbranch.skillarticles.data.remote.res

import com.squareup.moshi.Json

class CategoryRes(
    @Json(name = "id")
    val id: String,
    @Json(name = "title")
    val title: String,
    @Json(name = "icon")
    val icon: String
)


