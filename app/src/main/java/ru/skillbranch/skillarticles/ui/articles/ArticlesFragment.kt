package ru.skillbranch.skillarticles.ui.articles

import android.util.Log
import androidx.fragment.app.viewModels
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.ui.base.Binding
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand

class ArticlesFragment : BaseFragment<ArticlesViewModel> {
    override val viewModel: ArticlesViewModel by viewModels()
    override val layout: Int = R.layout.fragment_articles
    override val binding: ArticlesBinding by lazy{ ArticlesBinding()}

    private val articlesAdapter = ArticlesAdapter{
        item ->
        Log.e("ArticlesFragment", "click on article: ${item.id}")
        val action = ArticlesFragmentDirections.actionNavArticlesToPageArticle(
            item.id,
            item.author,
            item.authorAvatar,
            item.category,
            item.categoryIcon,
            item.date,
            item.poster,
            item.title
        )t
        viewModel.navigate(
            NavigationCommand.To(action.actionId, action.argument)
        )
    }

    override fun setupViews() {
        with(tv_articles) {
            layoutManager = LinearLayoutManager(context)
            adapter = articlesAdapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
    }
    inner class ArticlesBinding: Binding(){
        private var articles: List<ArticleItemData> by RenderProp(emptyList()){
            articlesAdapter.submitList(it)
        }
        override fun bind(data: IViewModelState) {
            data as ArticlesState
            articles = data.articles
        }
    }
}