package ru.skillbranch.skillarticles.ui.delegates

import android.util.Log
import ru.skillbranch.skillarticles.ui.base.Binding
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class RenderProp<T: Any>(
    var value: T,
    private val needInit: Boolean = true,
    private val onChange: ((T) -> Unit)? = null
) : ReadWriteProperty<Binding, T> {

    private val listeners: MutableList<() -> Unit> = mutableListOf()

    fun bind(){
        if(needInit) onChange?.invoke(value)
    }

    override fun getValue(thisRef: Binding, property: KProperty<*>): T = value

    override fun setValue(thisRef: Binding, property: KProperty<*>, value: T) {
        if(value == this.value) return
        Log.e("RenderProp","setValue() ${property.toString()}")
        this.value = value
        onChange?.invoke(this.value)
        if(listeners.isNotEmpty()) listeners.forEach{ it.invoke()}
    }

    fun addListener( listener: () -> Unit){
        Log.e("RenderProp addListener","${listener.toString()}")
        listeners.add(listener)
    }

    private fun registerDelegate(thisRef: Binding, name: String, delegate: RenderProp<T>){
        thisRef.delegates[name] = delegate
    }

    operator fun provideDelegate(
       thisRef: Binding,
       prop: KProperty<*>
    ): ReadWriteProperty<Binding, T>{
        val delegate = RenderProp(value,true,onChange)
        registerDelegate(thisRef, prop.name, delegate)
        return delegate
    }
}

