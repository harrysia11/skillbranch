package ru.skillbranch.skillarticles.viewmodels.articles

import android.util.Log
import androidx.lifecycle.*
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import ru.skillbranch.skillarticles.data.local.entities.CategoryData
import ru.skillbranch.skillarticles.data.repositories.ArticleFilter
import ru.skillbranch.skillarticles.data.repositories.ArticlesRepository
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Notify
import java.util.concurrent.Executors

class ArticlesViewModel(handle: SavedStateHandle): BaseViewModel<ArticlesState>(handle,ArticlesState()) {
    private val TAG = "ArticlesViewModel"
    private val repository = ArticlesRepository
    private val listConfig by lazy{
        PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(10)
            .setInitialLoadSizeHint(50)
            .setPrefetchDistance(30)
            .build()
    }

    private val listData = Transformations.switchMap(state) {
        val filter = it.toArticleFilter()
        return@switchMap buildPageList(repository.rawQueryArticles(filter))

    }

    fun observeList(
        owner: LifecycleOwner,
        isBookmark: Boolean = false,
        onChange: (list: PagedList<ArticleItem>) -> Unit
    ){
        updateState { it.copy(isBookmark = false) }
        listData.observe(owner,
            Observer{onChange(it)}
        )
    }

    private fun buildPageList(
        dataFactory: DataSource.Factory<Int,ArticleItem>
    ): LiveData<PagedList<ArticleItem>> {
        val builder = LivePagedListBuilder<Int, ArticleItem>(
            dataFactory,
            listConfig
        )

        if(isEmptyFilter()){
            builder.setBoundaryCallback(ArticlesBoundaryCallback(
                ::zeroLoadingHandle,
                ::itemAtEndHandle
            ))
        }
        return builder
            .setFetchExecutor(Executors.newSingleThreadExecutor())
            .build()
    }

    private fun isEmptyFilter(): Boolean =
        currentState.searchQuery.isNullOrEmpty()
                && !currentState.isBookmark
                && !currentState.isHashtagSearch
                && currentState.selectedCategories.isEmpty()

    private fun itemAtEndHandle(lastLoadedArticle: ArticleItem) {
        Log.e(TAG,"itemAtEndHanldle")
        val item = viewModelScope.launch (Dispatchers.IO){
            val items = repository.loadArticlesFromNetwork(
                start = lastLoadedArticle.id.toInt().inc(),
                size = listConfig.pageSize
            )
            if(items.isNotEmpty()){
                repository.insertArticlesToDb(items)
            }
            withContext(Dispatchers.Main) {
                notify(
                    Notify.TextMessage(
                        "Load from network articles from ${items.firstOrNull()?.data?.id} to ${items.lastOrNull()?.data?.id}"
                    )
                )
            }
        }
    }

    private fun zeroLoadingHandle() {
        Log.e(TAG,"zeroLoadingHandle")
        notify(Notify.TextMessage("Storage is empty"))
        viewModelScope.launch(Dispatchers.IO){
            val items = repository.loadArticlesFromNetwork(start = 0, size = listConfig.initialLoadSizeHint)
            if(items.isNotEmpty()){
                repository.insertArticlesToDb(items)
                listData.value?.dataSource?.invalidate()
            }
        }
    }

    fun handleSearchMode(isSearch: Boolean) {
        updateState { it.copy(isSearch = isSearch) }
    }

    fun handleSearch(query: String?) {
        query ?: return
        updateState { it.copy(searchQuery = query,isHashtagSearch = query.startsWith("#",true)) }
    }

    fun handleToggleBookmark(articleId:String){
        viewModelScope.launch {
            repository.toggleBookmark(articleId)
        }
    }

    fun observeTags(owner: LifecycleOwner,onChange: (list: List<String>) -> Unit) {
        repository.findTags().observe(owner, Observer(onChange))
    }

    fun handleSuggestion(tag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.incrementTagUseCount(tag)
        }
    }

    fun applyCategories(selectedCategories: List<String>) {
        updateState { it.copy(selectedCategories = selectedCategories) }
    }

    fun observeCategories(owner: LifecycleOwner, onChange:(list: List<CategoryData>) -> Unit) {
       repository.findCategoriesData().observe(owner, Observer(onChange)) }
    }


private fun ArticlesState.toArticleFilter(): ArticleFilter =
    ArticleFilter(
        search = searchQuery,
        isBookmark = isBookmark,
        categories = selectedCategories,
        isHashtag = isHashtagSearch
    )

data class ArticlesState(
    val isSearch: Boolean = false,
    val searchQuery: String? = null,
    val isBookmark: Boolean = false,
    val isHashtagSearch : Boolean = false,
    val selectedCategories: List<String> = emptyList(),
    val isLoading: Boolean = true
): IViewModelState

class ArticlesBoundaryCallback(
    val zeroLoadingHandle: () -> Unit,
    val itemAtEndHanldle: (ArticleItem) -> Unit
): PagedList.BoundaryCallback<ArticleItem>(){
    override fun onZeroItemsLoaded() {
        zeroLoadingHandle()
    }

    override fun onItemAtEndLoaded(itemAtEnd: ArticleItem) {
        itemAtEndHanldle(itemAtEnd)
    }

}