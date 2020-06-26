package ru.skillbranch.skillarticles.data.repositories

import android.util.Log
import androidx.paging.DataSource
import androidx.paging.PositionalDataSource
import ru.skillbranch.skillarticles.data.LocalDataHolder
import ru.skillbranch.skillarticles.data.NetworkDataHolder
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import java.lang.Thread.sleep


object ArticlesRepository {

    private val local = LocalDataHolder
    private val network = NetworkDataHolder

    fun allArticles(): ArticlesDataFactory =
        ArticlesDataFactory(ArticleStrategy.AllArticles(::findArticlesByRange))

    fun searchArticles(searchQuery: String): ArticlesDataFactory =
        ArticlesDataFactory(ArticleStrategy.SearchArticle(::findArticlesByTitle, searchQuery))

    private fun findArticlesByRange(start: Int, size: Int) = local.LOCAL_ARTICLE_ITEMS
        .drop(start)
        .take(size)

    private fun findArticlesByTitle(start: Int, size: Int, searchQuery: String) =
        local.LOCAL_ARTICLE_ITEMS
            .asSequence()
            .filter { it.title.contains(searchQuery, true) }
            .drop(start)
            .take(size)
            .toList()

    fun loadArticlesFromNetwork(start: Int, size: Int): List<ArticleItem> {
        return network.NETWORK_ARTICLE_ITEMS
            .drop(start)
            .take(size)
            .apply { sleep(500) }
    }

    fun insertArticlesToDb(articles: List<ArticleItem>) {
        local.LOCAL_ARTICLE_ITEMS.addAll(articles).apply { sleep(100) }
    }

    fun updateBookmark(id: String, checked: Boolean) {
        var article = local.LOCAL_ARTICLE_ITEMS.find { it.id == id }
        val articleId = local.LOCAL_ARTICLE_ITEMS.indexOf(article)
        article = article?.copy(isBookmark = checked)
        article ?: return
        local.LOCAL_ARTICLE_ITEMS[articleId] = article
    }

    fun loadBookmarks(): ArticlesDataFactory =
        ArticlesDataFactory(ArticleStrategy.BookmarksArticles(::loadBookmarks))

    fun loadBookmarks(start: Int, size: Int): List<ArticleItem> {
        return local.LOCAL_ARTICLE_ITEMS.filter { it.isBookmark }
            .drop(start)
            .take(size)
            .apply { sleep(300) }
    }

    fun loadBookmarks(start: Int, size: Int, queryString: String): List<ArticleItem> {
        return local.LOCAL_ARTICLE_ITEMS.filter {
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

class ArticlesDataFactory( val strategy: ArticleStrategy): DataSource.Factory<Int, ArticleItem>(){
    override fun create(): DataSource<Int, ArticleItem> {
        return ArticleDataSource(strategy)
    }
}

class ArticleDataSource(private val strategy: ArticleStrategy): PositionalDataSource<ArticleItem>(){
    private val tag: String = "ArticlesRepository"

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<ArticleItem>) {
        val result = strategy.getItems(params.startPosition,params.loadSize)
        Log.e(tag,"load range: from ${params.startPosition} to  ${params.startPosition + params.loadSize} result ${result.size}")
        callback.onResult(result)
    }

    override fun loadInitial(
        params: LoadInitialParams,
        callback: LoadInitialCallback<ArticleItem>
    ) {
       val result = strategy.getItems(params.requestedStartPosition, params.requestedLoadSize)
        Log.e(tag,"loadInitial: from ${params.requestedStartPosition} result ${result.size}")
        callback.onResult(result, params.requestedStartPosition)
    }
}

sealed class ArticleStrategy(){
    abstract fun getItems(start: Int, size: Int): List<ArticleItem>

    class AllArticles(
        private val itemProvider: (Int, Int) -> List<ArticleItem>
    ): ArticleStrategy(){
        override fun getItems(start: Int, size: Int): List<ArticleItem> {
            return itemProvider(start, size)
        }
    }
    class SearchArticle(
        private val itemProvider: (Int, Int, String) -> List<ArticleItem>,
        private val query: String
    ): ArticleStrategy(){
        override fun getItems(start: Int, size: Int): List<ArticleItem> {
            return itemProvider(start,size,query)
        }
    }

    class BookmarksArticles(private val itemProvider: (Int, Int) -> List<ArticleItem>): ArticleStrategy(){
        override fun getItems(start: Int, size: Int): List<ArticleItem> {
            return itemProvider(start, size)
        }

    }
    class SearchBookmarks(private val itemProvider: (Int, Int, String) -> List<ArticleItem>, private val query: String): ArticleStrategy(){
        override fun getItems(start: Int, size: Int): List<ArticleItem> {
            return itemProvider(start, size,query)
        }
    }
}