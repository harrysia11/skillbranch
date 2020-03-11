package ru.skillbranch.skillarticles.markdown

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import ru.skillbranch.skillarticles.data.repositories.MarkdownElement
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.extensions.setPaddingOptionally
import ru.skillbranch.skillarticles.ui.custom.markdown.MarkdownImageView
import kotlin.properties.Delegates

class MarkdownContentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
    private lateinit var elements: List<MarkdownElement>

    //for restore
    private var ids = arrayListOf<Int>()

    var textSize  by Delegates.observable(24f){
        _, old, value ->
        if(value == old) return@observable
        children.forEach {
            it as IMarkdownView
            it.fontSize = value
        }
    }
    var isLoading: Boolean = true
   // val padding //8dp

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var usedHeight = paddingTop
        val width = View.getDefaultSize(suggestedMinimumWidth,widthMeasureSpec)

        children.forEach {
            measureChild(it,widthMeasureSpec, heightMeasureSpec)
            usedHeight += it.measuredHeight
        }
        usedHeight += paddingBottom
        setMeasuredDimension(width,usedHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var usedHeigth = paddingTop
        val bodyWidth = r - l -paddingLeft - paddingRight
        val left = paddingLeft
        val right = paddingLeft + bodyWidth

        children.forEach {
            if(it is MarkdownTextView){
                it.layout(
                    left - paddingLeft/2,
                    usedHeigth,
                    right - paddingRight/2,
                    usedHeigth + it.measuredHeight
                )
            }else{
                it.layout(
                    left,
                    usedHeigth,
                    right,
                    usedHeigth + it.measuredHeight
                )
            }
            usedHeigth += it.measuredHeight
        }
    }

    fun setContent(content: List<MarkdownElement>) {
        elements = content
        content.forEach {
            when(it){
                is MarkdownElement.Text -> {
                    val tv = MarkdownTextView(context,textSize).apply{
                        setPaddingOptionally(left = context.dpToIntPx(8),right = context.dpToIntPx(8))
                        setLineSpacing(fontSize * 0.5f,1f)
                    }
                    MarkdownBuilder(context)
                        .markdownToSpan(it)
                        .run{
                            tv.setText(this, TextView.BufferType.SPANNABLE)
                        }
                    addView(tv)
                }
                is MarkdownElement.Image -> {
                    val iv = MarkdownImageView(
                        context,
                        textSize,
                        it.image.url,
                        it.image.text,
                        it.image.alt
                    )
                    addView(iv)
                }
                is MarkdownElement.Scroll -> {
                    // TODO
                }

            }
        }
    }

    fun renderSearchResult(searchResult: List<Pair<Int, Int>>) {
        //TODO implement me
    }

    fun renderSearchPosition(
        searchPosition: Pair<Int, Int>?
    ) {
        //TODO implement me
    }

    fun clearSearchResult() {
        //TODO implement me
    }

    fun setCopyListener(listener: (String) -> Unit) {
        //TODO implement me
    }
}