package ru.skillbranch.skillarticles.data.local

import android.content.SharedPreferences
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import ru.skillbranch.skillarticles.App
import ru.skillbranch.skillarticles.data.JsonConverter
import ru.skillbranch.skillarticles.data.JsonConverter.moshi
import ru.skillbranch.skillarticles.data.delegates.PrefDelegate
import ru.skillbranch.skillarticles.data.delegates.PrefLiveDelegate
import ru.skillbranch.skillarticles.data.delegates.PrefLiveObjDelegate
import ru.skillbranch.skillarticles.data.delegates.PrefObjDelegate
import ru.skillbranch.skillarticles.data.models.AppSettings
import ru.skillbranch.skillarticles.data.models.User

object PrefManager {


    internal val preferences : SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(App.applicationContext())
    }

    var profile: User? by PrefObjDelegate(JsonConverter.moshi.adapter(User::class.java))
    var accessToken by PrefDelegate("")
    var refreshToken by PrefDelegate("")
    var isDarkMode by PrefDelegate(false)
    var isBigText by PrefDelegate(false)
    val isAuthLive: LiveData<Boolean> by lazy {

        val token by PrefLiveDelegate("accessToken", "", preferences)
        token.map { it.isNotEmpty()}
    }

        val profileLive: LiveData<User?> by PrefLiveObjDelegate("profile",
            moshi.adapter(User::class.java),
            preferences)

        val appSettings: LiveData<AppSettings> = MediatorLiveData<AppSettings>().apply {

            val isDarkModeLive: LiveData<Boolean> by PrefLiveDelegate("isDarkMode", false, preferences)
            val isBigTextLive: LiveData<Boolean> by PrefLiveDelegate("isBigText", false, preferences)
            value = AppSettings()

            addSource(isDarkModeLive){
                value = value!!.copy(isDarkMode = it)
            }
            addSource(isBigTextLive){
                value = value!!.copy(isBigText = it)
            }
        }.distinctUntilChanged()


        fun clearAll(){
            preferences.edit().clear().apply()
        }

//    fun getAppSettings(): LiveData<AppSettings>{
//        return MutableLiveData(AppSettings(isDarkMode,isBigText))
//    }

        fun isAuth(): MutableLiveData<Boolean> {
            return MutableLiveData(false)
        }


    fun removeAvatar() {
        profile = profile!!.copy(avatar = "")
    }

    fun editProfile(name: String, about: String) {
        profile = profile!!.copy(name = name,about = about)
    }

//        fun setAuth(auth: Boolean): Unit {
//
//        }
//
//        fun updateSettings(appSettings: AppSettings) {
//
//            val mode = if(appSettings.isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
//            else AppCompatDelegate.MODE_NIGHT_NO
//
//            AppCompatDelegate.setDefaultNightMode(mode)
//
//        }
    }

