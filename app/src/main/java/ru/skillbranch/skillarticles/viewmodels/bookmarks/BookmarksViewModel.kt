package ru.skillbranch.skillarticles.viewmodels.bookmarks

import android.util.Log
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import ru.skillbranch.skillarticles.data.repositories.ArticleStrategy
import ru.skillbranch.skillarticles.data.repositories.ArticlesDataFactory
import ru.skillbranch.skillarticles.data.repositories.ArticlesRepository
import ru.skillbranch.skillarticles.viewmodels.articles.ArticlesBoundaryCallback
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import java.util.concurrent.Executors

class BookmarksViewModel(handler:SavedStateHandle ): BaseViewModel<BookmarksState>(handler, BookmarksState()),
    IViewModelState {

    private val repository = ArticlesRepository

    private val listConfig by lazy{
        PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(5)
            .setInitialLoadSizeHint(10)
            .setPrefetchDistance(5)
            .build()
    }

    private val listData = Transformations.switchMap(state) {
        when {
            it.isSearch && !it.queryString.isNullOrBlank() -> buildPageList(repository.searchBookmarks(currentState.queryString!!))
            else -> buildPageList(repository.loadBookmarks())
        }
    }

    private fun buildPageList(
        dataFactory: ArticlesDataFactory
        ): LiveData<PagedList<ArticleItem>> {
        val builder = LivePagedListBuilder<Int, ArticleItem>(dataFactory, listConfig)

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

    private fun itemAtEndHandle(lastLoadedArticle: ArticleItem) {
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
        onChange: (list: PagedList<ArticleItem>) -> Unit
    ){
        listData.observe(owner,
            Observer{onChange(it)}
        )
    }

    fun handleToggleBookmark(articlesId: String, bookmark: Boolean) {
        repository.updateBookmark(articlesId,bookmark)
        listData.value?.dataSource?.invalidate()
    }

    fun handleSearchMode(isSearch: Boolean) {
        updateState{it.copy(isSearch = isSearch)}
    }

    fun handleSearch(newQuery: String?) {
        newQuery ?: return
        updateState{it.copy(queryString = newQuery)}

    }

}
data class BookmarksState(
    var isSearch: Boolean = false,
    var queryString: String? = null
): IViewModelState
