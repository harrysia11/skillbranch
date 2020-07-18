package ru.skillbranch.skillarticles.ui.bookmarks

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_bookmarks.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import ru.skillbranch.skillarticles.ui.articles.ArticlesAdapter
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.ui.base.MenuItemHolder
import ru.skillbranch.skillarticles.ui.base.ToolbarBuilder
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand
import ru.skillbranch.skillarticles.viewmodels.bookmarks.BookmarksViewModel

class BookmarksFragment : BaseFragment<BookmarksViewModel>() {
    override val viewModel: BookmarksViewModel by viewModels()
    override val layout: Int = R.layout.fragment_bookmarks

    //lateinit var viewModel: BookmarksViewModel
    private val articlesAdapter = ArticlesAdapter(
        { item:ArticleItem, isToggleBookmark: Boolean ->
            if (isToggleBookmark) {
                viewModel.handleToggleBookmark(item.id)
            } else {
                val direction = BookmarksFragmentDirections.actionNavBookmarksToPageArticle2(
                    item.id,
                    item.author,
                    item.authorAvatar,
                    item.category,
                    item.categoryIcon,
                    item.title,
                    item.date,
                    item.poster
                )
                //       findNavController().navigate(direction)
                viewModel.navigate(
                    NavigationCommand.To(direction.actionId, direction.arguments)
                )
            }
        }
    )

    override val prepareToolbar: (ToolbarBuilder.() -> Unit)?
            = {
        addMenuItem(
            MenuItemHolder(
                "Search",
                R.id.action_search,
                R.drawable.ic_search_black_24dp,
                R.layout.search_view_layout
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val menuItem = menu.findItem(R.id.action_search) ?: return

        val searchView = menuItem.actionView as SearchView

        val modelState = viewModel.state.value ?: return

        if(modelState.isSearch) {
            menuItem.expandActionView()
            searchView.setQuery(modelState.queryString, false)
            if (modelState.queryString.isNullOrBlank()) searchView.requestFocus()

        }else searchView.clearFocus()

        menuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                viewModel.handleSearchMode(true)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                viewModel.handleSearchMode(false)
                return true
            }
        })

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.handleSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.handleSearch(newText)
                return true
            }
        })

        searchView.setOnCloseListener {
            viewModel.handleSearchMode(false)
            true
        }
    }

    override fun setupViews() {
        with(rv_bookmarks) {
            layoutManager = LinearLayoutManager(context)
            adapter = articlesAdapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
        viewModel.observeList(viewLifecycleOwner){
            articlesAdapter.submitList(it)
        }
    }

    private fun toggleIsBookmark(articlesId: String, isBookmark:Boolean){
        viewModel.handleToggleBookmark(articlesId, isBookmark)
    }

}
