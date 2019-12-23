package ru.skillbranch.skillarticles.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Checkable
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

class CheckableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    dcfStyleAttr:Int = 0
): ImageView(context, attrs,dcfStyleAttr),Checkable, View.OnClickListener{

    private var checked = false
    companion object{
        private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
    }

    init {
        setOnClickListener(this)
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState:IntArray =  super.onCreateDrawableState(extraSpace + 1)
        if(isChecked) View.mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        return drawableState
    }


    override fun isChecked(): Boolean {
        return checked
    }

    override fun toggle() {
        isChecked = !checked
    }

    override fun setChecked(check: Boolean) {
        if (checked == check) return
        checked = check
        refreshDrawableState()
    }


    override fun onClick(v: View?) {
        toggle()
    }


}