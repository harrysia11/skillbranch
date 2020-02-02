package ru.skillbranch.skillarticles.ui.Base

import android.os.Bundle
import android.util.Log
import ru.skillbranch.skillarticles.ui.custom.delegates.RenderProp
import ru.skillbranch.skillarticles.viewmodels.Base.IViewModelState
import kotlin.reflect.KProperty

abstract class Binding {
    val delegates = mutableMapOf<String,RenderProp<out Any>>()

    abstract fun onFinishInflate()

    abstract  fun bind(data: IViewModelState)

    abstract fun saveUi(outState: Bundle)

    abstract fun restoreUi(savedState: Bundle)

    fun <A,B,C,D> dependsOn(
        vararg  fields: KProperty<*>,
        onChange: (A,B,C,D) -> Unit
    ){

        check(fields.size ==4 ){"Names size must be 4, current ${fields.size}"}
        val names = fields.map { it.name }
        Log.e("class Binding dependsOn","${names.toString()}")
        names.forEach {
            delegates[it]?.addListener { onChange(
                delegates[names[0]]?.value as A,
                delegates[names[1]]?.value as B,
                delegates[names[2]]?.value as C,
                delegates[names[3]]?.value as D

            ) }
        }
    }
}