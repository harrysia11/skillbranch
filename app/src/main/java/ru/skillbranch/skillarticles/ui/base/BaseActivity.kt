package ru.skillbranch.skillarticles.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions.circleCropTransform
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import kotlinx.android.synthetic.main.activity_root.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.viewmodels.base.*

abstract class BaseActivity<T: BaseViewModel<out IViewModelState>> : AppCompatActivity() {

    protected abstract val viewModel: T
    protected abstract val layout: Int
    lateinit var navController: NavController

    val toolbarBuilder = ToolbarBuilder()
    val bottombarBuilder = BottombarBuilder()

    abstract fun subscribeOnState(state: IViewModelState)

    // set listeners, turning views
//    abstract fun setupViews()
    abstract fun renderNotification(notify: Notify)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)
        setSupportActionBar(toolbar)
        viewModel.observeState(this) { subscribeOnState(it) }
        viewModel.observeNotifications(this) { renderNotification(it) }
        viewModel.observeNavigation(this) { subscribeOnNavigation(it) }
        viewModel.observeLoading(this){renderLoading(it)}

        navController = findNavController(R.id.nav_host_fragment)
    }

    open fun renderLoading(loadingState: Loading) {
        when(loadingState) {
            Loading.SHOW_LOADING -> progress.isVisible = true
            Loading.HIDE_LOADING -> {
                progress.isVisible = true
                // TODO blocking UI
            }
            Loading.SHOW_BLOCKING_LOADING -> progress.isVisible = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveState()
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        viewModel.restoreState()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun subscribeOnNavigation(command: NavigationCommand) {
        when (command) {
            is NavigationCommand.To -> {
                navController.navigate(
                    command.destination,
                    command.args,
                    command.options,
                    command.extras
                )
            }
            is NavigationCommand.FinishLogin -> {
                navController.navigate(R.id.finish_login)
                if (command.privateDestination != null) navController.navigate(command.privateDestination)
            }

            is NavigationCommand.StartLogin -> {
                navController.navigate(
                    R.id.start_login,
                    bundleOf("private_destination" to (command.privateDestination ?: -1))
                )
            }
        }

    }
}

class ToolbarBuilder(){

    var title: String? = null
    var subtitle: String? = null
    var logo: String? = null
    var visibility: Boolean = true
    var items: MutableList<MenuItemHolder> = mutableListOf()

    fun setTitle(title: String): ToolbarBuilder{
        this.title = title
        return this
    }

    fun setSubTitle(subtitle: String): ToolbarBuilder{
        this.subtitle = subtitle
        return this
    }
    fun setLogo(logo: String): ToolbarBuilder{
        this.logo = logo
        return this
    }
    fun setVisibility(visibility: Boolean): ToolbarBuilder{
        this.visibility = visibility
        return this
    }

    fun addMenuItem(item: MenuItemHolder): ToolbarBuilder{
        this.items.add(item)
        return this
    }

    fun invalidate(): ToolbarBuilder{
        this.title = null
        this.subtitle = null
        this.logo = null
        this.visibility = true
        this.items.clear()
        return this
    }
    fun prepare(prepareFn: (ToolbarBuilder.() -> Unit)?): ToolbarBuilder{
        invalidate()
        prepareFn?.invoke(this)
        return this
    }
    fun build(context: FragmentActivity){
        context.appbar.setExpanded(true, true)

        with(context.toolbar){
            if(this@ToolbarBuilder.title != null) title = this@ToolbarBuilder.title
            subtitle = this@ToolbarBuilder.subtitle
            if(this@ToolbarBuilder.logo != null){
                val logoSize = context.dpToIntPx(40)
                val logoMargin = context.dpToIntPx(16)
                val logoPlaceholder = getDrawable(context, R.drawable.logo_placeholder)

                logo = logoPlaceholder

                val logo = children.last() as? ImageView
                if(logo != null){
                    logo.scaleType = ImageView.ScaleType.CENTER_CROP
                    (logo.layoutParams as? Toolbar.LayoutParams)?.let{
                        it.width = logoSize
                        it.height = logoSize
                        it.marginEnd = logoMargin
                        logo.layoutParams = it
                    }

                    Glide.with(context)
                        .load(this@ToolbarBuilder.logo)
                        .apply(circleCropTransform())
                        .override(logoSize)
                        .into(logo)
                }
            }else{
                logo = null
            }
        }
    }
}

data class MenuItemHolder(
    val title: String,
    val menuId:Int,
    val icon:Int,
    val actionViewLayout: Int? = null,
    val clickListener: ((MenuItem) -> Unit)? = null
)

class BottombarBuilder(){
    private var visible: Boolean = true
    private var views = mutableListOf<Int>()
    private var tempViews = mutableListOf<Int>()

    fun addView(layoutId: Int): BottombarBuilder{
        views.add(layoutId)
        return this
    }

    fun setVisibility( isVisible: Boolean): BottombarBuilder{
        visible = isVisible
        return this
    }
    fun prepare(prepareFn:(BottombarBuilder.() -> Unit)?): BottombarBuilder{
        prepareFn?.invoke(this)
        return this
    }
    fun invalidate(): BottombarBuilder{
        visible =true
        views.clear()
        return this
    }

    fun build(context: FragmentActivity){
        if(tempViews.isNotEmpty()){
            tempViews.forEach{
                val view = context.container.findViewById<View>(it)
                context.container.removeView(view)
            }
            tempViews.clear()
        }
        if(views.isNotEmpty()){
            val inflator = LayoutInflater.from(context)
            views.forEach {
                // container - это android.id="@+id/container" - из разметки activity_root
                val view = inflator.inflate(it, context.container, false)
                context.container.addView(view)
                tempViews.add(view.id)
            }
        }
        with(context.nav_view){
            isVisible = visible
            ((layoutParams as CoordinatorLayout.LayoutParams).behavior as HideBottomViewOnScrollBehavior)
                .slideUp(this)
        }

    }
}

