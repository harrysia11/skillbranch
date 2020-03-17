package ru.skillbranch.skillarticles.ui.custom.markdown

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import ru.skillbranch.skillarticles.data.repositories.MarkdownElement
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.extensions.groupByBounds
import ru.skillbranch.skillarticles.extensions.setPaddingOptionally
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
                is MarkdownElement.Image ->{
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
                        val sv = MarkdownCodeView(
                            context,
                            textSize,
                            it.blockCode.text
                        )
                        addView(sv)
                    }
                }
        }
    }

    fun renderSearchResult(searchResult: List<Pair<Int, Int>>) {
        children.forEach {
            view ->
            view as IMarkdownView
            view.clearSearchResult()
        }
        if(searchResult.isEmpty()) return

        val bounds = elements.map{ it.bounds}
        val result = searchResult.groupByBounds(bounds)

        children.forEachIndexed { index, view ->
            view as IMarkdownView
            view.renderSearchResult(result[index], elements[index].offset)
        }
    }

    fun renderSearchPosition(
        searchPosition: Pair<Int, Int>?
    ) {
       searchPosition ?: return
        val bounds = elements.map{
            it.bounds
        }

        val index = bounds.indexOfFirst { (start, end)  ->
            val boundRange = start..end
            val ( startPos,endPos) = searchPosition
            startPos in boundRange && endPos in boundRange
        }

        if(index == -1) return

        val view = getChildAt(index)
        view as IMarkdownView
        view.renderSearchPosition(searchPosition, elements[index].offset)

    }

    fun clearSearchResult() {
        children.forEach { view ->
            view as IMarkdownView
            view.clearSearchResult()
        }
    }

    fun setCopyListener(listener: (String) -> Unit) {
        //TODO implement me
    }
}


