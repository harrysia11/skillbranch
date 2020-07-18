package ru.skillbranch.skillarticles.viewmodels.article

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.skillbranch.skillarticles.data.models.CommentItemData
import ru.skillbranch.skillarticles.data.repositories.ArticleRepository
import ru.skillbranch.skillarticles.data.repositories.CommentsDataFactory
import ru.skillbranch.skillarticles.data.repositories.MarkdownElement
import ru.skillbranch.skillarticles.data.repositories.clearContent
import ru.skillbranch.skillarticles.extensions.data.toAppSettings
import ru.skillbranch.skillarticles.extensions.indexesOf
import ru.skillbranch.skillarticles.extensions.shortFormat
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand
import ru.skillbranch.skillarticles.viewmodels.base.Notify
import java.util.concurrent.Executors

class ArticleViewModel(
    handle: SavedStateHandle,
    private val articleId: String
) : BaseViewModel<ArticleState>(handle,ArticleState()), IArticleViewModel {

    private val repository = ArticleRepository
    private val TAG : String = "ArticleViewModel"
    private var clearContent: String? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val listConfig by lazy {
        PagedList.Config.Builder()
            .setEnablePlaceholders(true)
            .setPageSize(5)
            .build()
    }
    private val listData: LiveData<PagedList<CommentItemData>>
            = Transformations.switchMap(repository.findArticleCommentCount(articleId)){
                buildPageList(repository.loadAllComments(articleId,it))
    }


    init {

        subscribeOnDataSource(repository.findArticle(articleId)){ article, state ->
            article ?: return@subscribeOnDataSource null
            if(article.content == null) fetchContent()
            state.copy(
                sharedLink = article.sharedLink,
                title = article.title,
                category = article.category.title,
                categoryIcon = article.category.icon,
                date = article.date.shortFormat(),
                author = article.author,
                isBookmark = article.isBookmark,
                isLike =  article.isLike,
                content = article.content ?: emptyList(),
                isLoadingContent = article.content == null
            )
        }

        subscribeOnDataSource(repository.getAppSettings()){settings, state ->
            state.copy(
                isDarkMode = settings.isDarkMode,
                isBigText = settings.isBigText
            )
        }

        subscribeOnDataSource(repository.isAuth()){
            auth, state ->
            state.copy(isAuth = auth)
        }
    }

    private fun fetchContent() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.fetchArticleComment(articleId)
        }
    }


    override fun handleUpText() {
        repository.updateSettings(currentState.toAppSettings().copy(isBigText = true))
    }

    override fun handleDownText() {
        repository.updateSettings(currentState.toAppSettings().copy(isBigText = false))
    }

    override fun handleNightMode() {
        val settings = currentState.toAppSettings()
        Log.d(TAG, "isDarkMode ${currentState.isDarkMode}")
        repository.updateSettings(settings.copy(isDarkMode = !settings.isDarkMode))

    }

    override fun handleLike() {

        val isLiked = currentState.isLike
        val msg = if(!isLiked) Notify.TextMessage("Mark is like")
        else{
            Notify.ActionMessage("Don`t like it anymore",
                "No, still like it")
            {handleLike()}
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleLike(articleId)
            if(isLiked) repository.decrementLike(articleId) else repository.incrementLike(articleId)
            withContext(Dispatchers.IO){
                notify(msg)
            }
        }
    }

    override fun handleBookmark() {
        var msg = "Add to bookmarks"
        if(!currentState.isBookmark) msg = "Remove from bookmarks"

        viewModelScope.launch(Dispatchers.IO){
            repository.toggleBookmark(articleId)
            withContext(Dispatchers.Main){
                notify(Notify.TextMessage(msg))
            }
        }
    }

    override fun handleShare() {
        val msg = "Share is not implemented"
        notify(Notify.ErrorMessage(msg,"OK",null))


    }

    override fun handleToggleMenu() {
        updateState { it.copy(isShowMenu = !it.isShowMenu) }
    }

    override fun handleSearchMode(isSearch: Boolean) {
        updateState {
            it.copy(
                isSearch = isSearch,
                isShowMenu = false,
                searchPosition = 0
            )
        }
    }

    override fun handleSearch(query: String?) {
        query ?: return
        //
        if(clearContent == null && currentState.content.isNotEmpty()) {
            clearContent = currentState.content.clearContent()
        }

        val results = clearContent.indexesOf(query)
            .map{ it to it + query.length}
        updateState { it.copy(searchQuery = query,searchResults = results, searchPosition = 0) }
    }

    fun handleUpResult() {
        updateState { it.copy(searchPosition = it.searchPosition.dec()) }
    }

    fun handleDownResult() {
        updateState { it.copy(searchPosition = it.searchPosition.inc()) }
    }

    fun handleCopyCode(){
        notify(Notify.TextMessage("Code copy to clipboard"))
    }

    fun handleSendComment(comment: String) {
        if(!currentState.isAuth) navigate(NavigationCommand.StartLogin())
        else {
            viewModelScope.launch(Dispatchers.IO) {
                repository.sendMessage(articleId, comment, currentState.answerToSlug)
                withContext(Dispatchers.Main) {
                    updateState {
                        it.copy(answerTo = null, answerToSlug = null)
                    }
                }
            }
        }
    }

    fun observeList(
        owner: LifecycleOwner,
        onChange:( list: PagedList<CommentItemData>) -> Unit
    ){
        listData.observe(owner, Observer { onChange(it) })
    }

    private fun buildPageList(
        dataFactory: CommentsDataFactory
    ): LiveData<PagedList<CommentItemData>>{
        return LivePagedListBuilder<String, CommentItemData>(
            dataFactory,
            listConfig
        )
            .setFetchExecutor(Executors.newSingleThreadExecutor())
            .build()
    }

    fun handleCommentFocus() {
        updateState {
            it.copy(answerTo = null, answerToSlug = null)
        }
    }

    fun handleCommentFocus(hasFocus: Boolean) {
        updateState {
            it.copy(isShowBottombar = !hasFocus)
        }
    }

    fun handleReplyTo(slug: String, name: String){
        updateState{
            it.copy(answerToSlug = slug, answerTo = "Reply to $name")
        }
    }
}


data class ArticleState(
    val isAuth: Boolean = false,
    val isLoadingContent: Boolean = true,
    val isLoadingReviews: Boolean = true,
    val isLike: Boolean = false,
    val isBookmark: Boolean = false,
    val isShowMenu: Boolean = false,
    val isBigText: Boolean = false,
    val isDarkMode: Boolean = false,
    val isSearch: Boolean = false,
    val searchQuery: String? = null,
    val searchResults: List<Pair<Int, Int>> = emptyList(),
    val searchPosition: Int = 0,
    val sharedLink: String? = null,
    val title: String? = null,
    val category: String? = null,
    val categoryIcon: Any? = null,
    val date: String? = null,
    val author:Any? = null,
    val poster: String? = null,
    val content: List<MarkdownElement> = emptyList(),
    val commentsCount: Int = 0,
    val answerTo: String? = null,
    val answerToSlug: String? = null,
    val isShowBottombar: Boolean = true
) : IViewModelState {

    override fun save(outState: SavedStateHandle) {
        // TODO save state
        outState.set("isSearch", isSearch)
        outState.set("searchQuery", searchQuery)
        outState.set("searchResults", searchResults)
        outState.set("searchPosition", searchPosition)
    }

    override fun restore(saveState: SavedStateHandle): ArticleState {
        return copy(
            // TODO restore state
            isSearch = saveState["isSearch"] ?: false,
            searchQuery = saveState["searchQuery"],
            searchResults = saveState["searchResults"] ?: emptyList(),
            searchPosition = saveState["searchPosition"] ?: 0
        )

    }

}


