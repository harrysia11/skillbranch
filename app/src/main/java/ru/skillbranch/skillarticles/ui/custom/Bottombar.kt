package ru.skillbranch.skillarticles.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import ru.skillbranch.skillarticles.R

class Bottombar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttrs: Int = 0)
    : ConstraintLayout(context,attrs,defStyleAttrs) {

    init {
        View.inflate(context, R.layout.layout_bottombar,this)


    }

}