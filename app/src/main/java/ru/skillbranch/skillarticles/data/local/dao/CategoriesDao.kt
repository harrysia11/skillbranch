package ru.skillbranch.skillarticles.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import ru.skillbranch.skillarticles.data.local.entities.Category
import ru.skillbranch.skillarticles.data.local.entities.CategoryData

@Dao
interface CategoriesDao : BaseDao<Category> {

    @Query(
        """
        SELECT 
        category.title AS title
        ,category.icon
        ,category.category_id AS category_id
         ,COUNT(articles.category_id) AS article_count
         FROM articles_categories AS category
         INNER JOIN articles 
         ON articles.id = category.category_id
         GROUP BY category.category_id
         ORDER BY article_count DESC
    """
    )
    fun findAllCategoriesData(): LiveData<List<CategoryData>>

}