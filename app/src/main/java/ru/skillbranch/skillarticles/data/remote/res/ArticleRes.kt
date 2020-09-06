package ru.skillbranch.skillarticles.data.remote.res

import com.squareup.moshi.Json

data class ArticleRes(
    @Json(name = "data")
    val data: ArticleDataRes,
    @Json(name = "counts")
    val counts: ArticleCountsRes,
    @Json(name = "isActive")
    val isActive: Boolean = true
)






