package ru.skillbranch.skillarticles.markdown

import android.text.Spannable

interface IMarkdownView {
    var fontSize: Float
    val spannableContent: Spannable
}