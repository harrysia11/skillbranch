package ru.skillbranch.skillarticles.data.repositories

import android.util.Log
import androidx.paging.DataSource
import androidx.paging.PositionalDataSource
import ru.skillbranch.skillarticles.data.LocalDataHolder
import ru.skillbranch.skillarticles.data.NetworkDataHolder
import ru.skillbranch.skillarticles.data.models.ArticleItemData
import java.lang.Thread.sleep


object ArticlesRepository {

    private val local = LocalDataHolder
    private val network = NetworkDataHolder

    fun allArticles(): ArticlesDataFactory =
        ArticlesDataFactory(ArticleStrategy.AllArticles(::findArticlesByRange))

    fun searchArticles(searchQuery: String): ArticlesDataFactory =
        ArticlesDataFactory(ArticleStrategy.SearchArticle(::findArticlesByTitle, searchQuery))

    private fun findArticlesByRange(start: Int, size: Int) = local.localArticleItems
        .drop(start)
        .take(size)

    private fun findArticlesByTitle(start: Int, size: Int, searchQuery: String) =
        local.localArticleItems
            .asSequence()
            .filter { it.title.contains(searchQuery, true) }
            .drop(start)
            .take(size)
            .toList()

    fun loadArticlesFromNetwork(start: Int, size: Int): List<ArticleItemData> {
        return network.networkArticleItems
            .drop(start)
            .take(size)
            .apply { sleep(500) }
    }

    fun insertArticlesToDb(articles: List<ArticleItemData>) {
        local.localArticleItems.addAll(articles).apply { sleep(100) }
    }

    fun updateBookmark(id: String, checked: Boolean) {
        var article = local.localArticleItems.find { it.id == id }
        val articleId = local.localArticleItems.indexOf(article)
        article = article?.copy(isBookmark = checked)
        article ?: return
        local.localArticleItems[articleId] = article
    }

    fun loadBookmarks(): ArticlesDataFactory =
        ArticlesDataFactory(ArticleStrategy.BookmarksArticles(::loadBookmarks))

    fun loadBookmarks(start: Int, size: Int): List<ArticleItemData> {
        return local.localArticleItems.filter { it.isBookmark }
            .drop(start)
            .take(size)
            .apply { sleep(300) }
    }

    fun loadBookmarks(start: Int, size: Int, queryString: String): List<ArticleItemData> {
        return local.localArticleItems.filter {
            it.isBookmark && it.title.contains(
                queryString,
                true
            )
        }
            .drop(start)
            .take(size)
            .apply { sleep(300) }
    }

    fun searchBookmarks(queryString: String): ArticlesDataFactory =
        ArticlesDataFactory(ArticleStrategy.SearchBookmarks(::loadBookmarks, queryString))

}

class ArticlesDataFactory( val strategy: ArticleStrategy): DataSource.Factory<Int,ArticleItemData>(){
    override fun create(): DataSource<Int, ArticleItemData> {
        return ArticleDataSource(strategy)
    }
}

class ArticleDataSource(private val strategy: ArticleStrategy): PositionalDataSource<ArticleItemData>(){
    private val tag: String = "ArticlesRepository"

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<ArticleItemData>) {
        val result = strategy.getItems(params.startPosition,params.loadSize)
        Log.e(tag,"load range: from ${params.startPosition} to  ${params.startPosition + params.loadSize} result ${result.size}")
        callback.onResult(result)
    }

    override fun loadInitial(
        params: LoadInitialParams,
        callback: LoadInitialCallback<ArticleItemData>
    ) {
       val result = strategy.getItems(params.requestedStartPosition, params.requestedLoadSize)
        Log.e(tag,"loadInitial: from ${params.requestedStartPosition} result ${result.size}")
        callback.onResult(result, params.requestedStartPosition)
    }
}

sealed class ArticleStrategy(){
    abstract fun getItems(start: Int, size: Int): List<ArticleItemData>

    class AllArticles(
        private val itemProvider: (Int, Int) -> List<ArticleItemData>
    ): ArticleStrategy(){
        override fun getItems(start: Int, size: Int): List<ArticleItemData> {
            return itemProvider(start, size)
        }
    }
    class SearchArticle(
        private val itemProvider: (Int, Int, String) -> List<ArticleItemData>,
        private val query: String
    ): ArticleStrategy(){
        override fun getItems(start: Int, size: Int): List<ArticleItemData> {
            return itemProvider(start,size,query)
        }
    }
    //TODO bookmarks Strategy
    class BookmarksArticles(private val itemProvider: (Int, Int) -> List<ArticleItemData>): ArticleStrategy(){
        override fun getItems(start: Int, size: Int): List<ArticleItemData> {
            return itemProvider(start, size)
        }

    }
    class SearchBookmarks(private val itemProvider: (Int, Int, String) -> List<ArticleItemData>, private val query: String): ArticleStrategy(){
        override fun getItems(start: Int, size: Int): List<ArticleItemData> {
            return itemProvider(start, size,query)
        }
    }
}