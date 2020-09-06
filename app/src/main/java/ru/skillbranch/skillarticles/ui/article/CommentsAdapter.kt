package ru.skillbranch.skillarticles.ui.article

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import ru.skillbranch.skillarticles.data.models.CommentRes
import ru.skillbranch.skillarticles.ui.custom.CommentItemView

class CommentsAdapter(
    private val listener: (CommentRes) -> Unit
): PagedListAdapter<CommentRes,CommentVH>(CommentsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentVH {
//        val containerView = LayoutInflater.from(parent.context).inflate(R.layout.item_comment,parent,false)
//        return CommentVH(containerView,listener)
        return CommentVH(CommentItemView(parent.context),listener)
    }

    override fun onBindViewHolder(holder: CommentVH, position: Int) {
        holder.bind(getItem(position))
    }
}

class CommentsDiffCallback(): DiffUtil.ItemCallback<CommentRes>(){
    override fun areItemsTheSame(oldItem: CommentRes, newItem: CommentRes): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CommentRes, newItem: CommentRes): Boolean {
        return oldItem == newItem
    }
}

class CommentVH(
    override val containerView: View, val listener: (CommentRes) -> Unit
): RecyclerView.ViewHolder(containerView), LayoutContainer{
    fun bind(item: CommentRes?){
        (containerView as CommentItemView).bind(item)
        if(item != null){
            itemView.setOnClickListener { listener(item) }
//            tv_author_name.text = item.user.name
//        }else{
//            tv_author_name.text = "loading- need placeholder"
        }

    }
}