package ru.skillbranch.skillarticles.ui.articles

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import ru.skillbranch.skillarticles.ui.custom.ArticleItemView

class ArticlesAdapter(
    private val listener: (ArticleItem, Boolean) -> Unit

):PagedListAdapter<ArticleItem, ArticleVH>(ArticleDiffCallBack()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleVH {
    //    val containerView = LayoutInflater.from(parent.context).inflate(R.layout.item_article, parent, false)
    //    return ArticleVH(containerView)
        return ArticleVH(ArticleItemView(parent.context))
    }

    override fun onBindViewHolder(holder: ArticleVH, position: Int) {
        holder.bind(getItem(position), listener)
    }
}
class ArticleDiffCallBack: DiffUtil.ItemCallback<ArticleItem>(){
    override fun areItemsTheSame(oldItem: ArticleItem, newItem: ArticleItem): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: ArticleItem, newItem: ArticleItem): Boolean = oldItem == newItem
}

class ArticleVH(
    private val containerView: View
): RecyclerView.ViewHolder(containerView){

    fun bind(
        item: ArticleItem?,
        listener: (ArticleItem,Boolean) -> Unit
    ){
        (containerView as ArticleItemView).bind(item!!,listener) //  null
    }
}