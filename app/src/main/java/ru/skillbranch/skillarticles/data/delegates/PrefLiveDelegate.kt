package ru.skillbranch.skillarticles.data.delegates

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import com.squareup.moshi.JsonAdapter
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PrefLiveDelegate<T>(
    private val fieldKey: String,
    private val defValue: T,
    private val preferences: SharedPreferences
) :
    ReadOnlyProperty<Any?, LiveData<T>> {

    private var storedValue: LiveData<T>? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): LiveData<T> {
        if (storedValue == null) {
            storedValue =
                SharedPreferencesLiveData(
                    preferences,
                    fieldKey,
                    defValue
                )
        }
        return storedValue!!
    }


    inner class SharedPreferencesLiveData<T>(
        var sharedPref: SharedPreferences,
        var key: String,
        var defValue: T
    ) : LiveData<T>() {


        private val preferenceChangeListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, shKey ->
                if (shKey == key) {
                    value = readValue(defValue)
                }
            }

        override fun onActive() {
            super.onActive()
            value = readValue(defValue)
            sharedPref.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        }

        override fun onInactive() {
            super.onInactive()
            sharedPref.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        }

        private fun readValue(defaultValue: T): T {
            @Suppress("UNCHECKED_CAST")
            return when (defaultValue) {
                is Int -> sharedPref.getInt(key, defaultValue as Int) as T
                is Long -> sharedPref.getLong(key, defaultValue as Long) as T
                is Float -> sharedPref.getFloat(key, defaultValue as Float) as T
                is String -> sharedPref.getString(key, defaultValue as String) as T
                is Boolean -> sharedPref.getBoolean(key, defaultValue as Boolean) as T
                else -> throw Exception("This type can not be stored into Preferences")
            }
        }

    }
}

class PrefLiveObjDelegate<T>(
    private val fieldKey: String,
    private val adapter: JsonAdapter<T>,
    private val preferences: SharedPreferences
) :
    ReadWriteProperty<Any?, LiveData<T>> {

    private var storedValue: LiveData<T>? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): LiveData<T> {
        if (storedValue == null) {
            storedValue =
                SharedPreferencesLiveData(
                    preferences,
                    fieldKey,
                    adapter
                )
        }
        return storedValue!!
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: LiveData<T>) {
        storedValue = value
        with(preferences.edit()) {
            putString(fieldKey, storedValue?.value.let { adapter.toJson(it) })
        }.commit()
    }

    inner class SharedPreferencesLiveData<T>(
        var sharedPref: SharedPreferences,
        var key: String,
        var adapter: JsonAdapter<T>
    ) : LiveData<T>() {


        private val preferenceChangeListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, shKey ->
                if (shKey == key) {
                    value = readValue()
                }
            }

        override fun onActive() {
            super.onActive()
            value = readValue()
            sharedPref.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        }

        override fun onInactive() {
            super.onInactive()
            sharedPref.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        }

        private fun readValue(): T {
//            @Suppress("UNCHECKED_CAST")
//            return when (defaultValue) {
//                is Int -> sharedPref.getInt(key, defaultValue as Int) as T
//                is Long -> sharedPref.getLong(key, defaultValue as Long) as T
//                is Float -> sharedPref.getFloat(key, defaultValue as Float) as T
//                is String -> sharedPref.getString(key, defaultValue as String) as T
//                is Boolean -> sharedPref.getBoolean(key, defaultValue as Boolean) as T
//                else -> throw Exception("This type can not be stored into Preferences")
//            }

            return sharedPref.getString(key,null).let { adapter.fromJson(it)!! }
        }

    }
}

