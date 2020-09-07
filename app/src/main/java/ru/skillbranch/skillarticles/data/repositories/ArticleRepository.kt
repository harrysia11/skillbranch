package ru.skillbranch.skillarticles.data.repositories

import NetworkManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import ru.skillbranch.skillarticles.data.local.DbManager.db
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.local.dao.ArticleContentsDao
import ru.skillbranch.skillarticles.data.local.dao.ArticleCountsDao
import ru.skillbranch.skillarticles.data.local.dao.ArticlePersonalInfosDao
import ru.skillbranch.skillarticles.data.local.dao.ArticlesDao
import ru.skillbranch.skillarticles.data.local.entities.ArticleFull
import ru.skillbranch.skillarticles.data.models.AppSettings
import ru.skillbranch.skillarticles.data.models.CommentRes
import ru.skillbranch.skillarticles.data.remote.NetworkMonitor
import ru.skillbranch.skillarticles.data.remote.RestService
import ru.skillbranch.skillarticles.data.remote.err.NoNetworkError
import ru.skillbranch.skillarticles.data.remote.req.MessageReq
import ru.skillbranch.skillarticles.extensions.data.toArticleContent

interface IArticleRepository {
    fun findArticle(articleId: String): LiveData<ArticleFull>
    fun getAppSettings(): LiveData<AppSettings>
    fun isAuth(): MutableLiveData<Boolean>
    fun updateSettings(copy: AppSettings)

    suspend fun toggleLike(articleId: String)
    suspend fun toggleBookmark(articleId: String)
    suspend fun decrementLike(articleId: String)
    suspend fun incrementLike(articleId: String)
    suspend fun sendMessage(articleId: String, message: String, answerToMessageId: String?)
    suspend fun refreshCommentsCount(articleId: String)
    suspend fun fetchArticleContent(articleId: String)

    fun loadAllComments(articleId: String, total: Int, errHandler: (Throwable) -> Unit): CommentsDataFactory
    fun findArticleCommentCount(articleId: String): LiveData<Int>

}

object ArticleRepository : IArticleRepository {
    private val network = NetworkManager.api
    private val preferences = PrefManager

    private var articlesDao = db.articlesDao()
    private var articlePersonalDao = db.articlePersonalInfosDao()
    private var articleCountsDao = db.articleCountsDao()
    private var articleContentDao = db.articleContentsDao()

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun setupTestDao(
        articlesDao: ArticlesDao,
        articleCountsDao: ArticleCountsDao,
        articlePersonalDao: ArticlePersonalInfosDao,
        articleContentDao: ArticleContentsDao
    ) {
        this.articlesDao = articlesDao
        this.articlePersonalDao = articlePersonalDao
        this.articleCountsDao = articleCountsDao
        this.articleContentDao = articleContentDao
    }


    override fun findArticle(articleId: String): LiveData<ArticleFull> {
        return articlesDao.findFullArticle(articleId)
    }

    override fun getAppSettings(): LiveData<AppSettings> {
        return preferences.appSettings
    }

    override fun isAuth(): MutableLiveData<Boolean> = PrefManager.isAuth()

    override fun updateSettings(settings: AppSettings) {
        preferences.isBigText = settings.isBigText
        preferences.isDarkMode = settings.isDarkMode
    }

    override suspend fun toggleLike(articleId: String) {
        articlePersonalDao.toggleLikeOrInsert(articleId)
    }

    override suspend fun toggleBookmark(articleId: String) {
        articlePersonalDao.toggleBookmarkOrInsert(articleId)
    }

    override suspend fun decrementLike(articleId: String) {

        if(preferences.accessToken.isNullOrEmpty()) {
            articleCountsDao.decrementLike(articleId)
            return
        }
        try{
            val res = network.decrementLike(articleId, preferences.accessToken!!)
            articleCountsDao.updateLike(articleId,res.likeCount)
        }catch(e: Throwable){
            if( e is NoNetworkError){
                articleCountsDao.decrementLike(articleId)
                return
            }
            throw e
        }
    }

    override suspend fun incrementLike(articleId: String) {
        if(preferences.accessToken.isNullOrEmpty()) {
            articleCountsDao.incrementLike(articleId)
            return
        }
        try{
            val res = network.incrementLike(articleId, preferences.accessToken!!)
            articleCountsDao.updateLike(articleId,res.likeCount)
        }catch(e: Throwable){
            if( e is NoNetworkError){
                articleCountsDao.incrementLike(articleId)
                return
            }
            throw e
        }
    }

    override suspend fun sendMessage(
        articleId: String,
        message: String,
        answerToMessageId: String?) {
        val(_,messageCount) = network.sendMessage(
            articleId,
            MessageReq( message, answerToMessageId),
            preferences.accessToken!!
        )
        articleCountsDao.updateCommentsCount(articleId,messageCount)
    }

    override suspend fun refreshCommentsCount(articleId: String){
        val counts = network.loadArticleCounts(articleId)
        articleCountsDao.updateCommentsCount(articleId,counts.comments)
    }


    override suspend fun fetchArticleContent(articleId: String) {
        val content = network.loadArticleContent(articleId) //.apply { sleep(1500) }
        articleContentDao.insert(content.toArticleContent())
    }

    override fun findArticleCommentCount(articleId: String): LiveData<Int> {
        return articleCountsDao.getCommentsCount(articleId)
    }

    override fun loadAllComments(
        articleId: String,
        total: Int,
        errHandler: (Throwable) -> Unit
    ) = CommentsDataFactory(
        itemProvider = network,
        articleId = articleId,
        totalCount = total,
        errHandler = errHandler
    )

    suspend fun addBookmark(articleId: String) {
        if(isAuth().value == false) return
        if(NetworkMonitor.isConnected){
        //    network.incrementLike(articleId)
        }else{
            if(articlePersonalDao.isBookmarked(articleId)){

            }
        }
    }

    suspend fun removeBookmark(articleId: String) {

    }

//    override suspend fun loadCommentsByRange(
//        slug: String?,
//        size: Int,
//        articleId: String
//    ): List<CommentRes> {
//        val data = network.commentsData.getOrElse(articleId) { mutableListOf() }
//        return when {
//            slug == null -> data.take(size)
//
//            size > 0 -> data.dropWhile { it.slug != slug }
//                .drop(1)
//                .take(size)
//
//            size < 0 -> data.dropLastWhile { it.slug != slug }
//                .dropLast(1)
//                .takeLast(kotlin.math.abs(size))
//
//            else -> emptyList()
//        }.apply { sleep(500) }
//    }

}

class CommentsDataFactory(
    private val itemProvider: RestService,
    private val articleId: String,
    private val totalCount: Int,
    private val errHandler: (Throwable) -> Unit
) : DataSource.Factory<String?, CommentRes>() {
    override fun create(): DataSource<String?, CommentRes> =
        CommentsDataSource(itemProvider, articleId, totalCount, errHandler)
}

class CommentsDataSource(
    private val itemProvider: RestService,
    private val articleId: String,
    private val totalCount: Int,
    private val errHandler: (Throwable) -> Unit
) : ItemKeyedDataSource<String, CommentRes>() {
    private val TAG = CommentsDataSource::class.simpleName

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<CommentRes>
    ) {
        Log.e(
            TAG,
            "loadInitial InitialKey = ${params.requestedInitialKey} size = ${params.requestedLoadSize} totalCount = $totalCount"
        )
        try {
            // sync call
            val result = itemProvider.loadComments(
                articleId,
                params.requestedInitialKey,
                params.requestedLoadSize
            )
                .execute()
            callback.onResult(
                if (totalCount > 0) result.body()!! else emptyList(),
                0,
                totalCount
            )
        } catch (e: Throwable) {
            errHandler(e)
        }
    }

    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<CommentRes>) {
        try {
            val result = itemProvider.loadComments(
                articleId,
                params.key,
                params.requestedLoadSize
            ).execute()
            Log.e(TAG, "loadAfter key = ${params.key} size = ${params.requestedLoadSize}")
            callback.onResult(result.body()!!)
        }catch (e: Throwable){
            errHandler(e)
        }
    }

    override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<CommentRes>) {
        try {
            val result = itemProvider.loadComments(
                articleId,
                params.key,
                -1 * params.requestedLoadSize
            ).execute()
            Log.e(TAG, "loadBefore key = ${params.key} size = ${-1 * params.requestedLoadSize}")
            callback.onResult(result.body()!!)
        }catch (e: Throwable){
            errHandler(e)
        }
    }

    override fun getKey(item: CommentRes): String = item.id
}