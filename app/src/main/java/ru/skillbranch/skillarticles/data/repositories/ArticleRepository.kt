package ru.skillbranch.skillarticles.data.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import ru.skillbranch.skillarticles.data.NetworkDataHolder
import ru.skillbranch.skillarticles.data.local.DbManager.db
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.local.entities.ArticleFull
import ru.skillbranch.skillarticles.data.models.AppSettings
import ru.skillbranch.skillarticles.data.models.CommentItemData
import ru.skillbranch.skillarticles.data.models.User
import ru.skillbranch.skillarticles.extensions.data.toArticleContent
import java.lang.Thread.sleep

interface IArticleRepository {
    fun findArticle(articleId: String): LiveData<ArticleFull>
    fun getAppSettings(): LiveData<AppSettings>
    fun toggleLike(articleId: String)
    fun toggleBookmark(articleId: String)
    fun isAuth(): MutableLiveData<Boolean>
    fun loadCommentsByRange(slug: String?, size: Int,articleId: String): List<CommentItemData>
    fun sendMessage(articleId: String,text:String, answerToSlug: String?)
    fun loadAllComments(articleId: String, total: Int):CommentsDataFactory
    fun decrementLike(articleId: String)
    fun incrementLike(articleId: String)
    fun updateSettings(copy: AppSettings)
    fun fetchArticleComment(articleId: String)
    fun findArticleCommentCount(articleId: String): LiveData<Int>

}

object ArticleRepository : IArticleRepository {
    private val network = NetworkDataHolder
    private val preferences = PrefManager

    private val articlesDao = db.articlesDao()
    private val articlePersonalDao = db.articlePersonalInfosDao()
    private val articleCountsDao = db.articleCountsDao()
    private val articleContentDao = db.articlesContentDao()


    override fun findArticle(articleId: String): LiveData<ArticleFull> {
        return articlesDao.findFullArticle(articleId)
    }

    override fun toggleLike(articleId: String) {
        articlePersonalDao.toggleLikeOrInsert(articleId)
    }

    override fun toggleBookmark(articleId: String) {
        articlePersonalDao.toggleBookmarkOrInsert(articleId)
    }


    override fun decrementLike(articleId: String) {
        articleCountsDao.decrementLike(articleId)
    }

    override fun incrementLike(articleId: String) {
        articleCountsDao.incrementLike(articleId)
    }

    override fun updateSettings(copy: AppSettings) {
        // TODO implement me
    }

    override fun fetchArticleComment(articleId: String) {
        val content = network.loadArticleContent(articleId).apply { sleep(1500) }
        articleContentDao.insert(content.toArticleContent())
    }

    override fun findArticleCommentCount(articleId: String): LiveData<Int> {
        return articleCountsDao.getCommentsCount(articleId)
    }


    override fun getAppSettings(): LiveData<AppSettings> =
        PrefManager.getAppSettings() //from preferences

    override fun isAuth(): MutableLiveData<Boolean> = PrefManager.isAuth()

    override fun loadAllComments(articleId: String, total: Int) = CommentsDataFactory(
        itemProvider = ::loadCommentsByRange,
        articleId = articleId,
        totalCount = total
    )

    override fun loadCommentsByRange(
        slug: String?,
        size: Int,
        articleId: String
    ): List<CommentItemData> {
        val data = network.commentsData.getOrElse(articleId) { mutableListOf() }
        return when {
            slug == null -> data.take(size)

            size > 0 -> data.dropWhile { it.slug != slug }
                .drop(1)
                .take(size)

            size < 0 -> data.dropLastWhile { it.slug != slug }
                .dropLast(1)
                .takeLast(kotlin.math.abs(size))

            else -> emptyList()
        }.apply { sleep(500) }
    }

    override fun sendMessage(articleId: String, comment: String, answerToSlug: String?) {
        network.sendMessage(
            articleId, comment, answerToSlug,
            User("777", "John Doe", "https://skill-branch.ru/img/bot/android-category.png")
        )
        articleCountsDao.incrementCommentsCount(articleId)
    }
}

class CommentsDataFactory(
    private val itemProvider: (String?, Int, String) -> List<CommentItemData>,
    private val articleId: String,
    private val totalCount: Int
): DataSource.Factory<String?, CommentItemData>(){
    override fun create(): DataSource<String?, CommentItemData> = CommentsDataSource(itemProvider, articleId, totalCount)
}

class CommentsDataSource(
    private val itemProvider: (String?, Int, String) -> List<CommentItemData>,
    private val articleId: String,
    private val totalCount: Int
): ItemKeyedDataSource<String,CommentItemData>(){
    private val TAG = CommentsDataSource::class.simpleName

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<CommentItemData>
    ) {
        val result = itemProvider(params.requestedInitialKey, params.requestedLoadSize, articleId)
        Log.e(TAG,"loadInitial InitialKey = ${params.requestedInitialKey} size = ${params.requestedLoadSize} totalCount = $totalCount")
        callback.onResult(
            if(totalCount > 0) result else emptyList(),
            0,
            totalCount
        )
    }

    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<CommentItemData>) {
        val result = itemProvider(params.key, params.requestedLoadSize, articleId)
        Log.e(TAG,"loadAfter key = ${params.key} size = ${params.requestedLoadSize}")
        callback.onResult(result)
    }

    override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<CommentItemData>) {
        val result = itemProvider(params.key, -1 * params.requestedLoadSize, articleId)
        Log.e(TAG,"loadBefore key = ${params.key} size = ${-1 * params.requestedLoadSize}")
        callback.onResult(result)
    }

    override fun getKey(item: CommentItemData): String = item.slug
}