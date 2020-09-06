package ru.skillbranch.skillarticles.data.remote.res

import com.squareup.moshi.Json
import java.util.*

data class ArticleDataRes (
    @Json(name = "id")
    val id: String,
    @Json(name =  "date")
    val date: Date,
    @Json(name = "author")
    val author: AuthorRes,
    @Json(name = "title")
    val title: String,
    @Json(name = "description")
    val description: String,
    @Json(name = "category")
    val category: CategoryRes,
    @Json(name = "poster")
    val poster: String,
    @Json(name = "tags")
    val tags: List<String> = listOf()
)