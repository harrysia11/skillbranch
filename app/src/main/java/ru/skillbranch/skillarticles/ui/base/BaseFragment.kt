package ru.skillbranch.skillarticles.ui.base

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_root.*
import ru.skillbranch.skillarticles.ui.RootActivity
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Loading

abstract class BaseFragment<T: BaseViewModel<out IViewModelState>> : Fragment(){

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var _mockRoot: RootActivity? = null

    val root: RootActivity
        get() = _mockRoot ?: activity as RootActivity

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    abstract val viewModel: T
    open val binding:Binding? = null
    protected abstract val layout: Int

    open val prepareToolbar: (ToolbarBuilder.() -> Unit)? = null
    open val prepareBottombar: (BottombarBuilder.() -> Unit)? = null

    val toolbar
        get() = root.toolbar

    abstract fun setupViews()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(layout,container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.restoreState()
        if (savedInstanceState != null) {
            binding?.restoreUi(savedInstanceState)
        }

        viewModel.observeState(viewLifecycleOwner){binding?.bind(it)}

        if(binding?.isInflated == false) binding?.onFinishInflate()

        viewModel.observeNotifications(viewLifecycleOwner){root.renderNotification(it)}
        viewModel.observeNavigation(viewLifecycleOwner){root.viewModel.navigate(it)}
        viewModel.observeLoading(viewLifecycleOwner){renderLoading(it)}

    }


    override fun onViewStateRestored(savedInstanceState: Bundle?) {

        super.onViewStateRestored(savedInstanceState)
        // перенесено из onViewCreated()
        // по причине - toolbar и bottombar могут не существовать в момент вызова onViewCreated()

        root.toolbarBuilder
            .invalidate()
            .prepare(prepareToolbar)
            .build(root)

        root.bottombarBuilder
            .invalidate()
            .prepare(prepareBottombar)
            .build(root)

        setupViews()

        binding?.rebind()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveState()
        binding?.saveUi(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if(root.toolbarBuilder.items.isNotEmpty()){
            for((index, menuHolder) in root.toolbarBuilder.items.withIndex()){
                val item = menu.add(0,menuHolder.menuId, index,menuHolder.title)
                item.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
                    .setIcon(menuHolder.icon)
                    .setOnMenuItemClickListener {
                        menuHolder.clickListener?.invoke(it)?.let { true } ?: false
                    }
                try {
                    if (menuHolder.actionViewLayout != null) item.setActionView(menuHolder.actionViewLayout)
                }catch (e:Exception) {
                    Log.e("BaseFragment", e.localizedMessage)
                }
            }
        }else{
            menu.clear()
        }

        super.onPrepareOptionsMenu(menu)
    }

    open fun renderLoading(loadingState: Loading){
        root.renderLoading(loadingState)
    }
}