package ru.skillbranch.skillarticles.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import ru.skillbranch.skillarticles.data.local.entities.ArticleContent

@Dao
interface ArticleContentsDao: BaseDao<ArticleContent> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insert(obj:ArticleContent): Long

}