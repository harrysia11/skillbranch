package ru.skillbranch.skillarticles.ui.articles

import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.provider.BaseColumns
import android.view.Menu
import android.view.MenuItem
import android.widget.AutoCompleteTextView
import android.widget.CursorAdapter
import android.widget.SearchView
import android.widget.SimpleCursorAdapter
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_articles.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.data.local.entities.CategoryData
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.ui.base.Binding
import ru.skillbranch.skillarticles.ui.base.MenuItemHolder
import ru.skillbranch.skillarticles.ui.base.ToolbarBuilder
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.viewmodels.articles.ArticlesState
import ru.skillbranch.skillarticles.viewmodels.articles.ArticlesViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand

class ArticlesFragment : BaseFragment<ArticlesViewModel>() {
    override val viewModel: ArticlesViewModel by viewModels()
    override val layout: Int = R.layout.fragment_articles
    override val binding: ArticlesBinding by lazy{ ArticlesBinding()}
    private val args: ArticlesFragmentArgs by navArgs()
    private lateinit var suggestionAdapter: SimpleCursorAdapter

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
        addMenuItem(
            MenuItemHolder(
                "Filter",
                R.id.action_filter,
                R.drawable.ic_filter_list_blac_24,
                null
            ){
                menuItem ->
                val action = ArticleFragmentDirections.choseCategory(
                    binding.selectedCategories.toTypedArray(),
                    binding.categories.toTypedArray()
                )
                viewModel.navigate(NavigationCommand.To(action.actionId,action.arguments))
            }
        )
    }

    private fun toggleIsBookmark(articlesId: String){
        viewModel.handleToggleBookmark(articlesId)
    }

    private val articlesAdapter:ArticlesAdapter = ArticlesAdapter { item,isToggleBookmark ->
            if (isToggleBookmark) {
                viewModel.handleToggleBookmark(item.id)
            } else {
                val direction = ArticlesFragmentDirections.actionNavArticlesToPageArticle(
                    item.id,
                    item.author,
                    item.authorAvatar,
                    item.category,
                    item.categoryIcon,
                    item.title,
                    item.date,
                    item.poster
                )
                viewModel.navigate(
                    NavigationCommand.To(direction.actionId, direction.arguments)
                )
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        suggestionAdapter = SimpleCursorAdapter(
            context,
            android.R.layout.simple_list_item_1,
            null,
            arrayOf("tag"),
            intArrayOf(android.R.id.text1),
            CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        )

        suggestionAdapter.setFilterQueryProvider { constraint -> populateAdapter(constraint) }
        setHasOptionsMenu(true)
    }

    private fun populateAdapter(constraint: CharSequence?): Cursor {
        val cursor = MatrixCursor(arrayOf(BaseColumns._ID,"tag"))
        if(constraint.isNullOrBlank()) return cursor

        val currentCursor = suggestionAdapter.cursor
        currentCursor.moveToFirst()
        for(i in 0 until currentCursor.count){
            val tagValue = currentCursor.getString(1) // from column "tag"
            if(tagValue.contains(constraint,true)){
                cursor.addRow(arrayListOf(i,tagValue))
                currentCursor.moveToNext()
            }
        }

        return cursor
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val menuItem = menu.findItem(R.id.action_search) ?: return

        val searchView = menuItem.actionView as SearchView
        if(binding.isSearch){
            menuItem.expandActionView()
            searchView.setQuery(binding.searchQuery, false)
            if(binding.isSearch) searchView.requestFocus()
            else searchView.clearFocus()
        }

        val autoTv = searchView.findViewById<AutoCompleteTextView>(R.id.search_src_text)
        autoTv.threshold = 1

        searchView.suggestionsAdapter = suggestionAdapter
        searchView.setOnSuggestionListener(object:SearchView.OnSuggestionListener{
            override fun onSuggestionSelect(position: Int): Boolean  = false

            override fun onSuggestionClick(position: Int): Boolean {
                suggestionAdapter.cursor.moveToPosition(position)
                val tag = suggestionAdapter.cursor.getString(1)
                searchView.setQuery(tag,true)
                viewModel.handleSuggestion(tag)
                return false
            }

        })

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
        with(rv_articles) {
            layoutManager = LinearLayoutManager(context)
            adapter = articlesAdapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
        viewModel.observeList(viewLifecycleOwner, args.isBookmark
        ){
            articlesAdapter.submitList(it)
        }

        viewModel.observeTags(viewLifecycleOwner){
            binding.tags = it
        }

        viewModel.observeCategories(viewLifecycleOwner){
            binding.categories = it
        }
    }
    inner class ArticlesBinding: Binding(){
        var categories: List<CategoryData> = emptyList()
        var selectedCategories: List<String> by RenderProp(emptyList())
        var searchQuery: String? = null
        var isSearch: Boolean = false
        var isLoading: Boolean by RenderProp(true) {
            // TODO show shimmer on rv_list
        }

        var isHashTagSearch: Boolean by RenderProp(false)
        var tags: List<String> by RenderProp(emptyList())

        override fun bind(data: IViewModelState) {
            data as ArticlesState
            isSearch = data.isSearch
            searchQuery = data.searchQuery
            isLoading = data.isLoading
            isHashTagSearch = data.isHashtagSearch
            selectedCategories = data.selectedCategories

        }

        override val afterInflated: (() -> Unit)? = {
            dependsOn<Boolean, List<String>>(::isHashTagSearch, ::tags) {ihs,tags ->
                val cursor = MatrixCursor(
                    arrayOf(BaseColumns._ID,"tag")
                )
                if(ihs && tags.isNotEmpty()){
                    for((counter, tag) in tags.withIndex()){
                        cursor.addRow(kotlin.collections.arrayListOf(counter,tag))
                    }
                }
                suggestionAdapter.changeCursor(cursor)
            }
        }

        // TODO save UI
    }
}