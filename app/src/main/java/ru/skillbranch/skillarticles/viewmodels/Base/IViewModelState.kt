package ru.skillbranch.skillarticles.viewmodels.Base

import android.os.Bundle

interface IViewModelState {
    fun save(outState: Bundle)
    fun restore(saveState: Bundle): IViewModelState
}