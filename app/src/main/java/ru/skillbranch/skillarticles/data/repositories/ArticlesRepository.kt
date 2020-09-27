package ru.skillbranch.skillarticles.data.repositories

import NetworkManager
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.sqlite.db.SimpleSQLiteQuery
import ru.skillbranch.skillarticles.data.local.DbManager.db
import ru.skillbranch.skillarticles.data.local.dao.*
import ru.skillbranch.skillarticles.data.local.entities.*
import ru.skillbranch.skillarticles.data.remote.res.ArticleRes
import ru.skillbranch.skillarticles.data.remote.res.CategoryRes
import ru.skillbranch.skillarticles.extensions.data.toArticle
import ru.skillbranch.skillarticles.extensions.data.toArticleContent
import ru.skillbranch.skillarticles.extensions.data.toArticleCounts

interface IArticlesRepository {

    suspend fun loadArticlesFromNetwork(start: String? =null, size: Int = 10): Int
    suspend fun insertArticlesToDb(articles: List<ArticleRes>)
    suspend fun toggleBookmark(articleId: String): Boolean
    fun findTags(): LiveData<List<String>>
    fun findCategoriesData(): LiveData<List<CategoryData>>
    fun rawQueryArticles(filter: ArticleFilter): DataSource.Factory<Int, ArticleItem>
    suspend fun incrementTagUseCount(tag: String)

}

object ArticlesRepository: IArticlesRepository {

    private val network = NetworkManager.api
    private var articlesDao: ArticlesDao = db.articlesDao()
    private var articleCountsDao: ArticleCountsDao = db.articleCountsDao()
    private var categoriesDao: CategoriesDao = db.categoriesDao()
    private var tagsDao: TagsDao = db.tagsDao()
    private var articlePersonalDao: ArticlePersonalInfosDao = db.articlePersonalInfosDao()
    private  var articlesContentDao: ArticleContentsDao = db.articleContentsDao()

    override suspend fun loadArticlesFromNetwork(start: String?, size: Int): Int {

        val items = network.articles(start, size)
        if(items.isNotEmpty()) insertArticlesToDb(items)
        return items.size
    }

    override suspend fun insertArticlesToDb(articles: List<ArticleRes>) {
        articlesDao.upsert(articles.map {
            it.data.toArticle()
        })
        articleCountsDao.upsert(articles.map {
            it.counts.toArticleCounts()
        })
        val refs = articles.map { it.data }
            .fold(mutableListOf<Pair<String, String>>()) { acc, res ->
                acc.also { list -> list.addAll(res.tags.map { res.id to it }) }
            }

        val tags = refs.map { it.second }
            .distinct()
            .map { Tag(it) }

        tagsDao.insert(tags)
        tagsDao.insertRefs(refs.map { ArticleTagXRef(it.first, it.second) })

        val categoriesRes = articles.map { it.data.category }.distinct()

        categoriesDao.insert(categoriesRes.map{it.toCategory()})
    }

    override suspend fun toggleBookmark(articleId: String): Boolean {
        return articlePersonalDao.toggleBookmarkOrInsert(articleId)
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

    override suspend fun incrementTagUseCount(tag: String) {
        tagsDao.incrementTagUseCount(tag)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun setupTestDao(
                    articlesDao: ArticlesDao,
                     articleCountsDao: ArticleCountsDao,
                     categoriesDao: CategoriesDao,
                     tagsDao: TagsDao,
                     articlePersonalDao: ArticlePersonalInfosDao,
                    articlesContentDao: ArticleContentsDao){

        this.articlesDao = articlesDao
        this.articleCountsDao = articleCountsDao
        this.categoriesDao = categoriesDao
        this.tagsDao = tagsDao
        this.articlePersonalDao = articlePersonalDao
    }

    suspend fun findLastArticleId(): String? {
        return articlesDao.findLastArticleId()
    }

    suspend fun fetchArticleContent(articleId: String) {
        val content = network.loadArticleContent(articleId)
        articlesContentDao.insert(content.toArticleContent())
    }

    suspend fun removeArticleContent(articleId: String) {
        articleCountsDao.removeArticleContent(articleId)
    }


}

private fun CategoryRes.toCategory()  = Category(categoryId = this.id,
icon = this.icon, title =  this.title)


class ArticleFilter(
        val search: String? = null,
        val isBookmark: Boolean = false,
        val categories: Set<String> = setOf(),
        val isHashtag: Boolean = false
    ) {
        fun toQuery(): String {
            val queryBuilder = QueryBuilder()
            queryBuilder.table("ArticleItem")

            if (search != null && !isHashtag) queryBuilder.appendWhere("title LIKE '%$search%'")

            if (search != null && isHashtag) {
                queryBuilder.innerJoin("article_tag_x_ref AS refs", "refs.a_id = id")
                queryBuilder.appendWhere("refs.t_id = '$search'")
            }

            if (isBookmark) queryBuilder.appendWhere("is_bookmark = 1")

            if (categories.isNotEmpty()) queryBuilder.appendWhere(
                "category_id IN (${categories.joinToString(
                    "\",\"","\"","\""
                )})"
            )

            queryBuilder.orderBy("date")

            return queryBuilder.build()
        }
    }

    class QueryBuilder() {
        private var table: String? = null
        private var selectColumns: String = "*"
        private var joinTables: String? = null
        private var whereCondition: String? = null
        private var order: String? = null

        fun build(): String {
            check(table != null) { "table must be not null" }
            val strBuilder = StringBuilder("SELECT $selectColumns")
                .append(" FROM $table")

            if (joinTables != null) strBuilder.append(joinTables)

            if (whereCondition != null) strBuilder.append(whereCondition)

            if (order != null) strBuilder.append(order)

            return strBuilder.toString()
        }

        fun appendWhere(condition: String, logic: String = " AND "): QueryBuilder {
            if (whereCondition.isNullOrEmpty()) this.whereCondition = " WHERE  $condition "
            else whereCondition += " $logic $condition "
            return this
        }

        fun table(table: String): QueryBuilder {
            this.table = table
            return this
        }

        fun orderBy(column: String, isDesc: Boolean = true): QueryBuilder {
            order = " ORDER BY $column ${if (isDesc) " DESC" else " ASC"}"
            return this
        }

        fun innerJoin(table: String, on: String): QueryBuilder {
            if (joinTables.isNullOrEmpty()) joinTables = " INNER JOIN $table ON $on"
            else joinTables += " INNER JOIN $table ON $on "
            return this
        }

    }
