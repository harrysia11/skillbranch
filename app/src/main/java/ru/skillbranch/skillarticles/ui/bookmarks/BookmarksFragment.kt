package ru.skillbranch.skillarticles.ui.bookmarks

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_bookmarks.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.ui.articles.ArticlesAdapter
import ru.skillbranch.skillarticles.viewmodels.bookmarks.BookmarksViewModel

/**
 * A simple [Fragment] subclass.
 */
class BookmarksFragment : Fragment() {

    companion object {
        fun newInstance() = BookmarksFragment()
    }

    lateinit var viewModel: BookmarksViewModel
    private val articlesAdapter = ArticlesAdapter(
        { Log.e("Bookmarks", "click on article ${it.id}") },
        ::toggleIsBookmark
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        viewModel = ViewModelProviders.of(this).get(BookmarksViewModel::class.java)

        return inflater.inflate(R.layout.fragment_bookmarks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(rv_bookmarks) {
            layoutManager = LinearLayoutManager(context)
            adapter = articlesAdapter
        }
        viewModel.observeList(viewLifecycleOwner){
            articlesAdapter.submitList(it)
        }
    }

    private fun toggleIsBookmark(articlesId: String, isBookmark:Boolean){
     //   viewModel.handleToggleBookmark(articlesId,isBookmark)
    }

}
