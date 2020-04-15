package ru.skillbranch.skillarticles.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.SearchView
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_root.*
import kotlinx.android.synthetic.main.fragment_article.*
import kotlinx.android.synthetic.main.layout_bottombar.*
import kotlinx.android.synthetic.main.layout_submenu.*
import kotlinx.android.synthetic.main.search_view_layout.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.data.repositories.MarkdownElement
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.extensions.hideKeyboard
import ru.skillbranch.skillarticles.extensions.setMarginOptionally
import ru.skillbranch.skillarticles.ui.article.IArticleView
import ru.skillbranch.skillarticles.ui.base.BaseActivity
import ru.skillbranch.skillarticles.ui.base.Binding
import ru.skillbranch.skillarticles.ui.delegates.ObserveProp
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.viewmodels.articles.ArticleState
import ru.skillbranch.skillarticles.viewmodels.articles.ArticleViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Notify
import ru.skillbranch.skillarticles.viewmodels.base.ViewModelFactory

class RootActivity : BaseActivity<ArticleViewModel>(),
    IArticleView {

    override val layout: Int = R.layout.activity_root
    override val viewModel: ArticleViewModel by viewModels{
        ViewModelFactory(
            owner =  this,
            params = "0"
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public override val binding: ArticleBinding by lazy { ArticleBinding() }


    override fun setupViews() {
        setupToolbar()
        setupBottombar()
        setupSubmenu()
    }

    override fun showSearchBar() {
        bottombar.setSearchState(true)
        scroll.setMarginOptionally(bottom = dpToIntPx(56))
    }

    override fun hideSearchBar() {
        bottombar.setSearchState(false)
        scroll.setMarginOptionally(bottom = dpToIntPx(0))
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        Log.e("onCreateOptionsMenu","onCreateOptionsMenu()")
//       // setContentView(R.layout.activity_root)
//      //  setSupportActionBar(toolbar)
//
//        val vmFactory = ViewModelFactory("0")
//        viewModel = ViewModelProviders.of(this,vmFactory).get(ArticleViewModel::class.java)
//        viewModel.observeState(this){
//            renderUi(it)
//
//        }
//
//        viewModel.observeNotifications(this){
//            renderNotification(it)
//        }
//
//    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        Log.e("onCreateOptionsMenu","onCreateOptionsMenu()")
        menuInflater.inflate(R.menu.menu_search,menu)
        val menuItem = menu?.findItem(R.id.action_search)
        val searchView = (menuItem?.actionView as? SearchView)
        searchView?.queryHint = getString(R.string.article_search_placeholder)

        if(binding.isSearch){
            menuItem?.expandActionView()
            searchView?.setQuery(binding.searchQuery,false)

            if(binding.isFocusedSearch) searchView?.requestFocus() else searchView?.clearFocus()
        }

        menuItem?.setOnActionExpandListener(object: MenuItem.OnActionExpandListener{
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                viewModel.handleSearchMode(false)
                return true
            }

            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                viewModel.handleSearchMode(true)
                return true

            }
        })

        searchView?.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.handleSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.handleSearch(newText)
                return true
            }

        })
        return true
        //super.onCreateOptionsMenu(menu)
    }

    override fun renderNotification(notify: Notify) {
        val snackbar:Snackbar = Snackbar.make(coordinator_container,notify.message,Snackbar.LENGTH_LONG)
            .setAnchorView(bottombar)

            when(notify){
                is Notify.TextMessage -> {}

                is Notify.ActionMessage -> {
                    snackbar.setActionTextColor(getColor(R.color.color_accent_dark))
                    snackbar.setAction(notify.actionLabel){
                        notify.actionHandler.invoke()
                    }
                }
                is Notify.ErrorMessage -> {
                     with(snackbar){   setBackgroundTint(getColor(R.color.design_default_color_error))
                         setTextColor(getColor(android.R.color.white))
                         setActionTextColor(getColor(android.R.color.white))
                         setAction(notify.errLabel){
                             notify.errHandler?.invoke()
                         }
                    }
                }
            }
        snackbar.show()
    }

    private fun setupSubmenu() {

        btn_text_up.setOnClickListener{viewModel.handleUpText() }
        btn_text_down.setOnClickListener { viewModel.handleDownText() }
        switch_mode.setOnClickListener { viewModel.handleNightMode() }
    }

    private fun setupBottombar() {

        btn_like.setOnClickListener { viewModel.handleLike() }
        btn_bookmark.setOnClickListener { viewModel.handleBookmark() }
        btn_share.setOnClickListener { viewModel.handleShare() }
        btn_settings.setOnClickListener { viewModel.handleToggleMenu() }

        btn_result_up.setOnClickListener {
 //           if(search_view != null && search_view.hasFocus()) search_view.clearFocus()
            if(!tv_text_content.hasFocus()) tv_text_content.requestFocus()
            hideKeyboard(btn_result_up)
            viewModel.handleUpResult()
        }

        btn_result_down.setOnClickListener {
 //           if(search_view != null && search_view.hasFocus()) search_view.clearFocus()
            if(!tv_text_content.hasFocus()) tv_text_content.requestFocus()
            hideKeyboard(btn_result_down)
            viewModel.handleDownResult()
        }

        btn_search_close.setOnClickListener {
            viewModel.handleSearchMode(false)
            invalidateOptionsMenu()
            tv_text_content.clearSearchResult()
        }

    }

//    private fun renderUi(data: ArticleState){
//
//        if(data.isSearch) showSearchBar() else hideSearchBar()
//
//        if(data.searchResult.isNotEmpty()){
//            data.searchResult.forEach {
//                Log.e("renderSearchResult", it.toString())
//            }
//            renderSearchResult(data.searchResult)
//            renderSearchPosition(data.searchPosition)
//        }
//
//        btn_settings.isChecked = data.isShowMenu
//        if(data.isShowMenu) submenu.open() else submenu.close()
//
//        btn_like.isChecked = data.isLike
//        btn_bookmark.isChecked = data.isBookmark
//
//        switch_mode.isChecked = data.isDarkMode
//        delegate.localNightMode = if(data.isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
//
//        if(data.isBigText){
//            tv_text_content.textSize = 18f
//            btn_text_up.isChecked = true
//            btn_text_down.isChecked = false
//        }else{
//            tv_text_content.textSize = 14f
//            btn_text_up.isChecked = false
//            btn_text_down.isChecked = true
//        }
//
//        if(data.isLoadingContent || data.content.isEmpty() ){
//            tv_text_content.text = "long long text content..."
//        }else if(tv_text_content.text == "long long text content..."){
//            val content = data.content.first() as String
//            tv_text_content.setText(content, TextView.BufferType.SPANNABLE)
//            tv_text_content.movementMethod = ScrollingMovementMethod()
//        }
// //       tv_text_content.text = if(!data.isLoadingContent || data.content.isEmpty()) "long long text content..." else data.content.first() as String
//
//        toolbar.title = data.title ?: "loading..."
////        toolbar.subtitle = data.author?.let{ it as String ?: "noname..."}
//        toolbar.subtitle = data.category ?: "loading"
//        if(data.categoryIcon != null)toolbar.logo = getDrawable(data.categoryIcon as Int)
//
//    }


    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val logo:ImageView? = if( toolbar.childCount > 2)toolbar.getChildAt(2) as ImageView else null
        logo?.scaleType = ImageView.ScaleType.CENTER_CROP

        val lp: Toolbar.LayoutParams? = logo?.layoutParams as? Toolbar.LayoutParams
        lp?.let{
            it.width = this.dpToIntPx(40)
            it.height = this.dpToIntPx(40)
            it.marginEnd = this.dpToIntPx(16)
            logo.layoutParams = it
        }
    }

    private fun setupCopyListener(){
        tv_text_content.setCopyListener { copy ->
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied code", copy)
            clipboard.setPrimaryClip(clip)
            viewModel.handleCopyCode()
        }
    }

     inner class ArticleBinding() : Binding(){

         var isFocusedSearch: Boolean = false
         var searchQuery: String? = null

         private var isLoadingContent by ObserveProp(true) {
             Log.e("ArticleBinding", "isLoadingContent = $it")
         }

         private var isLike: Boolean by RenderProp(false) {
             btn_like.isChecked = it
         }
         private var isBookmark: Boolean by RenderProp(
             false
         ) { btn_bookmark.isChecked = it }
         private var isShowMenu: Boolean by RenderProp(
             false
         ) {
             btn_settings.isChecked = it
             if (it) submenu.open() else submenu.close()
         }

         private var title: String by RenderProp("loading") {
             toolbar.title = it
         }
         private var category: String by RenderProp("loading") {
             toolbar.subtitle = it
         }
         private var categoryIcon: Int by RenderProp(R.drawable.logo_placeholder) {
             toolbar.logo = getDrawable(it)
         }

         private var isBigText: Boolean by RenderProp(
             false
         ) {
             if (it) {
                 tv_text_content.textSize = 18f
                 btn_text_up.isChecked = true
                 btn_text_down.isChecked = false
             } else {
                 tv_text_content.textSize = 14f
                 btn_text_up.isChecked = false
                 btn_text_down.isChecked = true
             }
         }

         private var isDarkMode :Boolean by RenderProp(
             false,
             false
         ) {
             switch_mode.isChecked = it
             delegate.localNightMode =
                 if (it) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
         }

        var isSearch : Boolean by ObserveProp(false) {
            if (it){
                showSearchBar()
                with(toolbar){
                    (layoutParams as AppBarLayout.LayoutParams).scrollFlags =
                        AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
                }
            } else hideSearchBar()
            with(toolbar){
                (layoutParams as AppBarLayout.LayoutParams).scrollFlags =
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED
            }
        }

         private var searchResults: List<Pair<Int,Int>> by ObserveProp(
             emptyList()
         )
         {
             //   if(it != null && it.isNotEmpty()) renderSearchResult(it)
         }
         private var searchPosition: Int by ObserveProp(0)

         private var content: List<MarkdownElement> by ObserveProp(emptyList())
         {
             tv_text_content.isLoading = it.isEmpty()
             tv_text_content.setContent(it)

         }

         override fun onFinishInflate() {

             dependsOn<Boolean,Boolean,List<Pair<Int,Int>>,Int>(
                 ::isLoadingContent,
                 ::isSearch,
                 ::searchResults,
                 ::searchPosition
             ){
                 ilc,iss, sr, sp ->
//                 Log.e("onFinishInflate","${::isLoadingContent.name} $ilc ")
                 if( !ilc && iss ){
                     tv_text_content.renderSearchResult(sr)
                     tv_text_content.renderSearchPosition(sr.getOrNull(sp))
                 }
                 if(!ilc && !iss){
                     tv_text_content.clearSearchResult()
                 }
                 bottombar.bindSearchInfo(sr.size,sp)
             }

         }

         override fun bind(data: IViewModelState) {
             data as ArticleState

             isLike = data.isLike
             isBookmark = data.isBookmark
             isShowMenu = data.isShowMenu
             if(data.title != null) title = data.title
             if(data.category != null) category = data.category
             if(data.categoryIcon != null) categoryIcon = data.categoryIcon as Int
             isBigText = data.isBigText
             isDarkMode = data.isDarkMode

             isLoadingContent = data.isLoadingContent
             isSearch = data.isSearch
             searchQuery = data.searchQuery
             searchPosition = data.searchPosition
             searchResults = data.searchResults
             content = data.content

         }

         override fun saveUi(outState: Bundle) {
             outState.putBoolean(::isFocusedSearch.name, search_view?.hasFocus() ?: false)
         }

         override fun restoreUi(savedState: Bundle) {
             isFocusedSearch = savedState.getBoolean(::isFocusedSearch.name)
         }
     }
}
