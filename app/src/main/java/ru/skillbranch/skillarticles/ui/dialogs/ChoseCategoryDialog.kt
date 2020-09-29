package ru.skillbranch.skillarticles.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.skillbranch.skillarticles.R


class ChoseCategoryDialog : DialogFragment() {

    companion object{
        const val CHOOSE_CATEGORY_KEY = "CHOOSE_CATEGORY_KEY"
        const val SELECTED_CATEGORIES = "SELECTED_CATEGORIES"
    }

//    private val viewModel : ArticlesViewModel by activityViewModels()
    private val selected = mutableSetOf<String>()
    private val args: ChoseCategoryDialogArgs by navArgs()

    private val categoryAdapter = CategoryAdapter {
        categoryId: String, isChecked: Boolean ->
        if(isChecked) selected.add(categoryId)
        else selected.remove(categoryId)

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        selected.clear()
        selected.addAll(
            savedInstanceState?.getStringArray("checked") ?: args.selectedCategories
        )

        val categoryItems = args.categories.map{
            it.toItem(selected.contains(it.categoryId))
        }

        categoryAdapter.submitList(categoryItems)

        val listView = layoutInflater.inflate(R.layout.fragment_chose_category_dialog,null) as RecyclerView

        with(listView){
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Choose category")
            .setPositiveButton("Apply"){
//                    _,_ -> viewModel.applyCategories(selected.toSet())
                _,_ -> setFragmentResult(CHOOSE_CATEGORY_KEY, bundleOf(SELECTED_CATEGORIES to selected.toSet()))
            }
            .setNegativeButton("Reset"){
//                    _,_ -> viewModel.applyCategories(emptySet())
                    _,_ -> setFragmentResult(CHOOSE_CATEGORY_KEY, bundleOf(SELECTED_CATEGORIES to emptySet<String>()))
            }
            .setView(listView)
            .create()

//        val categories = args.categories.toList().map { "${it.title} (${it.articlesCount})" }.toTypedArray()
//        val checked = BooleanArray(args.categories.size){
//            args.selectedCategories.contains(args.categories[it].categoryId)
//        }
//
//        val adb = AlertDialog.Builder(requireContext())
//            .setTitle("Choose category")
//            .setPositiveButton("Apply"){ dialog,which ->
//                viewModel.applyCategories(selected)
//
//            }
//            .setNegativeButton("Reset"){dialog, which ->
//                viewModel.applyCategories(emptySet<String>())
//            }
//            .setMultiChoiceItems(categories,checked){dialog,which, isChecked ->
//                if(isChecked) selected.add(args.categories[which].categoryId)
//                else selected.remove(args.categories[which].categoryId)
//            }
//
//        return adb.create()


    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArray("checked", selected.toTypedArray())
        super.onSaveInstanceState(outState)
    }
}