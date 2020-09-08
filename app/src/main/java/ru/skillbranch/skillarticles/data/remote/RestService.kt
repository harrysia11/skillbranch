package ru.skillbranch.skillarticles.data.remote

import retrofit2.Call
import retrofit2.http.*
import ru.skillbranch.skillarticles.data.remote.req.LoginReq
import ru.skillbranch.skillarticles.data.remote.req.MessageReq
import ru.skillbranch.skillarticles.data.remote.res.*

interface RestService {
    //https://skill-articles.skill-branch.ru/api/v1/articles?last=articlesId&limit=10
    @GET("articles")
    suspend fun articles(
        @Query("last") articleId: String?,
        @Query("limit") limit: Int = 10
    ): List<ArticleRes>

    //https://skill-articles.skill-branch.ru/api/v1/articles/{articlesId}/content
    @GET("articles/{article}/content")
    suspend fun loadArticleContent(
        @Path("article") articleId: String): ArticleContentRes

    //https://skill-articles.skill-branch.ru/api/v1/articles/{articlesId}/messages?last=articlesId&limit=5
    @GET("articles/{article}/messages")
    fun loadComments(
        @Path("article") articleId: String,
        @Query("last") last: String? = null,
        @Query("limit") limit: Int = 5
        ) : Call<List<CommentRes>>

    //https://skill-articles.skill-branch.ru/api/v1/articles/{articlesId}/counts
    @GET("articles/{article}/counts")
    suspend fun loadArticleCounts(
        @Path("article") articleId: String
    ): ArticleCountsRes

    //https://skill-articles.skill-branch.ru/api/v1/auth/login
    @POST("auth/login")
    suspend fun login(
        @Body loginReq: LoginReq
    ): AuthRes

    //https://skill-articles.skill-branch.ru/api/v1/articles/{articleId}/messages
    @POST("articles/{article}/messages")
    suspend fun sendMessage(
        @Path("article") articleId: String,
        @Body message: MessageReq,
        @Header("Authorization") token: String
    ): MessageRes

    //https://skill-articles.skill-branch.ru/api/v1/articles/{articleId}/decrementLikes
    @POST("articles/{article}/decrementLikes")
    suspend fun decrementLike(
        @Path("article") articleId: String,
        @Header("Authorization") token: String
    ): LikeRes

    //https://skill-articles.skill-branch.ru/api/v1/articles/{articleId}/incrementLikes
    @POST("articles/{article}/incrementLikes")
    suspend fun incrementLike(
        @Path("article") articleId: String,
        @Header("Authorization") token: String
    ): LikeRes

    //https://skill-articles.skill-branch.ru/api/v1/articles/{articleId}/addBookmark
    @POST("articles/{article}/addBookmark")
    suspend fun addBookmark(
        @Path("article") articleId: String,
        @Header("Authorization") token: String
    ): MessageRes

    //https://skill-articles.skill-branch.ru/api/v1/articles/{articleId}/removeBookmark
    @POST("articles/{article}/addBookmark")
    suspend fun removeBookmark(
        @Path("article") articleId: String,
        @Header("Authorization") token: String
    ): MessageRes

}