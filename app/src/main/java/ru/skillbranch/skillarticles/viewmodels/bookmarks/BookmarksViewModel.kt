package ru.skillbranch.skillarticles.viewmodels.bookmarks

import android.util.Log
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.skillbranch.skillarticles.data.models.ArticleItemData
import ru.skillbranch.skillarticles.data.repositories.ArticleStrategy
import ru.skillbranch.skillarticles.data.repositories.ArticlesDataFactory
import ru.skillbranch.skillarticles.data.repositories.ArticlesRepository
import ru.skillbranch.skillarticles.viewmodels.articles.ArticlesBoundaryCallback
import java.util.concurrent.Executors

class BookmarksViewModel: ViewModel() {

    private val repository = ArticlesRepository

    private val listConfig by lazy{
        PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(5)
            .setInitialLoadSizeHint(10)
            .setPrefetchDistance(5)
            .build()
    }

    private val listData = buildPageList(repository.loadBookmarks())

    private fun buildPageList(
        dataFactory: ArticlesDataFactory
        ): LiveData<PagedList<ArticleItemData>> {
        val builder = LivePagedListBuilder<Int,ArticleItemData>(dataFactory, listConfig)

        if(dataFactory.strategy is ArticleStrategy.BookmarksArticles){
            builder.setBoundaryCallback(
                ArticlesBoundaryCallback(
                    ::zeroLoadingHandle,
                    ::itemAtEndHandle
                )
            )
        }
        return builder.setFetchExecutor(Executors.newSingleThreadExecutor()).build()

    }

    private fun itemAtEndHandle(lastLoadedArticle: ArticleItemData) {
        Log.e("Bookmarks","itemAtEndHanldle")
        val item = viewModelScope.launch (Dispatchers.IO){
            val items = repository.loadBookmarks(
                start = lastLoadedArticle.id.toInt().inc(),
                size = listConfig.pageSize
            )
            if(items.isNotEmpty()){
                listData.value?.dataSource?.invalidate()
            }
        }
    }

    private fun zeroLoadingHandle() {
        viewModelScope.launch(Dispatchers.IO){
            val items = repository.loadBookmarks(start = 0, size = listConfig.initialLoadSizeHint)
            if(items.isNotEmpty()){
                listData.value?.dataSource?.invalidate()
            }
        }
    }

    fun observeList(
        owner: LifecycleOwner,
        onChange: (list: PagedList<ArticleItemData>) -> Unit
    ){
        listData.observe(owner,
            Observer{onChange(it)}
        )
    }


}