package ru.skillbranch.skillarticles.data.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.PositionalDataSource
import androidx.sqlite.db.SimpleSQLiteQuery
import ru.skillbranch.skillarticles.data.LocalDataHolder
import ru.skillbranch.skillarticles.data.NetworkDataHolder
import ru.skillbranch.skillarticles.data.local.DbManager.db
import ru.skillbranch.skillarticles.data.local.dao.*
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import ru.skillbranch.skillarticles.data.local.entities.ArticleTagXRef
import ru.skillbranch.skillarticles.data.local.entities.CategoryData
import ru.skillbranch.skillarticles.data.local.entities.Tag
import ru.skillbranch.skillarticles.data.remote.res.ArticleRes
import ru.skillbranch.skillarticles.extensions.data.toArticle
import ru.skillbranch.skillarticles.extensions.data.toArticleCounts
import java.lang.Thread.sleep

interface IArticlesRepository {

    fun loadArticlesFromNetwork(start: Int = 0, size: Int): List<ArticleRes>
    fun insertArticlesToDb(articles: List<ArticleRes>)
    fun toggleBookmark(articleId: String)
    fun findTags(): LiveData<List<String>>
    fun findCategoriesData(): LiveData<List<CategoryData>>
    fun rawQueryArticles(filter: ArticleFilter): DataSource.Factory<Int, ArticleItem>
    fun incrementTagUseCount(tag: String)

}

object ArticlesRepository: IArticlesRepository {

    private val network = NetworkDataHolder
    private val articlesDao: ArticlesDao = db.articlesDao()
    private val articleCountsDao: ArticleCountsDao = db.articlesCountDao()
    private val categoriesDao: CategoriesDao = db.categoriesDao()
    private val tagsDao: TagsDao = db.tagsDao()
    private val articlePersonalDao:ArticlePersonalInfosDao = db.articlePersonalInfosDao()

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

    override fun loadArticlesFromNetwork(start: Int, size: Int): List<ArticleRes> =
        network.findArticlesItem(start,size)

    override fun insertArticlesToDb(articles: List<ArticleRes>) {
        articlesDao.upsert(articles.map{
            it.data.toArticle()
        })
        articleCountsDao.upsert(articles.map{
            it.counts.toArticleCounts()
        })
        val refs = articles.map { it.data }
            .fold(mutableListOf<Pair<String, String>>()) { acc, res ->
                acc.also { list -> list.addAll(res.tags.map { res.id to it }) }
            }

        val tags = refs.map{ it.second }
            .distinct()
            .map{ Tag(it) }

        tagsDao.insert(tags)
        tagsDao.insertRefs(refs.map{ ArticleTagXRef(it.first, it.second) })

        val categories = articles.map{it.data.category}

        categoriesDao.insert(categories)
    }

    override fun toggleBookmark(articleId: String) {
        articlePersonalDao.toggleBookmarkOrInsert(articleId)
    }

    override fun findTags(): LiveData<List<String>> {
        return tagsDao.findTags()
    }

    override fun findCategoriesData(): LiveData<List<CategoryData>> {
        return categoriesDao.findAllCategoriesData()
    }

    override fun rawQueryArticles(filter: ArticleFilter): DataSource.Factory<Int, ArticleItem> {
        return articlesDao.findArticlesByRaw(SimpleSQLiteQuery(filter.toQuery()))
    }

    override fun incrementTagUseCount(tag: String) {
        tagsDao.incrementTagUseCount(tag)
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

class ArticleFilter(
    val search: String? = null,
    val isBookmark: Boolean = false,
    val categories: List<String> = listOf(),
    val isHashtag: Boolean  = false
){
    fun toQuery(): String{
        val queryBuilder = QueryBuilder()
        queryBuilder.table("ArticleItem")

        if(search != null && !isHashtag) queryBuilder.appendWhere("title LIKE '%$search%'")

        if(search != null && isHashtag){
            queryBuilder.innerJoin("article_tag_x_ref AS refs", "refs.a_id = id")
            queryBuilder.appendWhere("refs.t_id = '$search'")
        }

        if(isBookmark) queryBuilder.appendWhere("is_bookmark = true")

        if(categories.isNotEmpty()) queryBuilder.appendWhere("category_id IN (${categories.joinToString(",")})")

        queryBuilder.orderBy("date")

        return queryBuilder.build()
    }
}

class QueryBuilder(){
    private var table: String? = null
    private var selectColumns: String = "*"
    private var joinTables: String? = null
    private var whereCondition: String? = null
    private var order: String? = null

    fun build(): String {
        check(table != null) { "table must be not null" }
        val strBuilder = StringBuilder("SELECT $selectColumns")
            .append("FROM $table")

        if(joinTables != null) strBuilder.append(joinTables)

        if(whereCondition != null) strBuilder.append(whereCondition)

        if(order != null) strBuilder.append(order)

        return strBuilder.toString()
    }

    fun appendWhere(condition: String, logic: String = "AND"):QueryBuilder{
        if(whereCondition.isNullOrEmpty()) this.whereCondition = "WHERE  $condition"
        else whereCondition += " $logic $condition "
        return this
    }

    fun table(table: String): QueryBuilder{
        this.table = table
        return this
    }

    fun orderBy(column: String, isDesc: Boolean = true) : QueryBuilder{
        order = "ORDER BY $column ${if(isDesc) "DESC" else "ASC"}"
        return  this
    }

    fun innerJoin(table: String, on: String): QueryBuilder{
        if(joinTables.isNullOrEmpty()) joinTables = "INNER JOIN $table ON $on"
        else joinTables += " INNER JOIN $table ON $on "
        return this
    }

}