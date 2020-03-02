package ru.skillbranch.skillarticles.data.delegates

import ru.skillbranch.skillarticles.data.local.PrefManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PrefDelegate<T>( private val defaultValue: T) {

    private var storedValue:T? = null

    operator fun provideDelegate(
       thisRef: PrefManager,
       prop: KProperty<*>
    ): ReadWriteProperty<PrefManager, T?> {
        val key = prop.name
        return object : ReadWriteProperty<PrefManager,T?>{
            override fun getValue(thisRef: PrefManager, property: KProperty<*>): T? {
                if(storedValue == null){
                    @Suppress("UNCHECKED_CAST")
                    storedValue = when(defaultValue){
                        is Int -> thisRef.prefrences.getInt(key, defaultValue as Int) as T
                        is Long -> thisRef.prefrences.getLong(key, defaultValue as Long) as T
                        is Float -> thisRef.prefrences.getFloat(key,defaultValue as Float) as T
                        is String -> thisRef.prefrences.getString(key, defaultValue as String) as T
                        is Boolean -> thisRef.prefrences.getBoolean(key, defaultValue as Boolean) as T
                        else -> throw Exception("This type can not be stored into Preferences")
                    }
                }
                return storedValue
            }

            override fun setValue(thisRef: PrefManager, property: KProperty<*>, value: T?) {
                with(thisRef.prefrences.edit()){
                    when(value){
                        is Int -> putInt(key, value)
                        is Long -> putLong(key,value)
                        is Float -> putFloat(key,value)
                        is String -> putString(key,value)
                        is Boolean -> putBoolean(key,value)
                        else -> throw Exception("This type can not be stored into Preferences")
                    }
                    apply()
                }
                storedValue = value
            }

        }
    }
}