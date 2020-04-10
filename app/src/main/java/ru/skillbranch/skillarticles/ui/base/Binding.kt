package ru.skillbranch.skillarticles.ui.base

import android.os.Bundle
import android.util.Log
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import kotlin.reflect.KProperty

abstract class Binding {
    val delegates = mutableMapOf<String, RenderProp<out Any>>()
    var isInflated = false

    open val afterInflated: (() -> Unit)? = null

    open fun onFinishInflate(){
        if(!isInflated){
            afterInflated?.invoke()
            isInflated = true
            rebind()
        }
    }

    fun rebind(){
        delegates.forEach{it.value.bind()}
    }

    abstract  fun bind(data: IViewModelState)

    open fun saveUi(outState: Bundle){
    }

    open fun restoreUi(savedState: Bundle){
    }

    @Suppress("UNCHECKED_CAST")
    fun <A,B,C,D> dependsOn(
        vararg  fields: KProperty<*>,
        onChange: (A,B,C,D) -> Unit
    ){
        @Suppress("UNCHECKED_CAST")
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