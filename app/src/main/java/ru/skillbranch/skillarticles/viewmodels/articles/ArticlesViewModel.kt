package ru.skillbranch.skillarticles.viewmodels.articles

import androidx.lifecycle.SavedStateHandle
import ru.skillbranch.skillarticles.data.repositories.ArticleRepository
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState

class ArticlesViewModel(handle: SavedStateHandle): BaseViewModel<ArticleState>(handle,ArticlesState()) {
    val repository = ArticleRepository

    init {
        subscribeOnDataSource(repository.loadArticles()){articles, state ->
            articles ?: return@subscribeOnDataSource null
            state.copy(articles = articles)
        }
    }
}
data class ArticlesState(
    val articles: List<ArticleItemData> = emptyList()
): IViewModelState