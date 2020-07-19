package ru.skillbranch.skillarticles.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import ru.skillbranch.skillarticles.data.local.entities.ArticleContent

@Dao
interface ArticleContentDao: BaseDao<ArticleContent> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override fun insert(obj:ArticleContent): Long

}