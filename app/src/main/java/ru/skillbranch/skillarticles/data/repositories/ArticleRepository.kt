package ru.skillbranch.skillarticles.data.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import ru.skillbranch.skillarticles.data.LocalDataHolder
import ru.skillbranch.skillarticles.data.NetworkDataHolder
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.local.entities.ArticlePersonalInfo
import ru.skillbranch.skillarticles.data.models.AppSettings
import ru.skillbranch.skillarticles.data.models.ArticleData
import ru.skillbranch.skillarticles.data.models.CommentItemData
import ru.skillbranch.skillarticles.data.models.User
import java.lang.Thread.sleep

object ArticleRepository {
    private val network = NetworkDataHolder
    private val prefrences = PrefManager

    fun loadArticleContent(articleId: String): LiveData<List<MarkdownElement>?> {
        return MutableLiveData(emptyList())
        }
    }
    fun getArticle(articleId: String): LiveData<ArticleData?> {
        return MutableLiveData(null)
    }

    fun loadArticlePersonalInfo(articleId: String): LiveData<ArticlePersonalInfo?> {
        return MutableLiveData(null)
    }

    fun getAppSettings(): LiveData<AppSettings> = PrefManager.getAppSettings() //from preferences

    fun updateSettings(appSettings: AppSettings) {
    }

    fun updateArticlePersonalInfo(info: ArticlePersonalInfo) {
    }

    fun isAuth(): MutableLiveData<Boolean> = PrefManager.isAuth()

    fun allComments(articleId: String,totalCount: Int) = CommentsDataFactory(
        itemProvider = ::loadCommentsByRange,
        articleId = articleId,
        totalCount = totalCount)

    private fun loadCommentsByRange(slug: String?,size: Int, articleId: String): List<CommentItemData>{
        val data = network.commentsData.getOrElse(articleId){ mutableListOf()}
        return when{
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

    fun sendComment(articleId: String, comment: String, answerToSlug: String?) {
        network.sendMessage(articleId,comment,answerToSlug,
            User("777", "John Doe","https://skill-branch.ru/img/bot/android-category.png")
        )
        local.incrementCommentsCount(articleId)
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
    private val TAG = "CommentsDataFactory"

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<CommentItemData>
    ) {
        val result = itemProvider(params.requestedInitialKey, params.requestedLoadSize, articleId)
        Log.e(TAG,"loadInitial InitialKeykey = ${params.requestedInitialKey} size = ${params.requestedLoadSize} totalCount = $totalCount")
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