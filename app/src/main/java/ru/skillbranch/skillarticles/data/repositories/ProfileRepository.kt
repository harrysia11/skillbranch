package ru.skillbranch.skillarticles.data.repositories

import NetworkManager
import androidx.lifecycle.LiveData
import okhttp3.MultipartBody
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.models.User

interface IProfileRepository{
    fun getProfile(): LiveData<User?>
    suspend fun removeAvatar()
    suspend fun editProfile(name: String, about: String)
}

object ProfileRepository : IProfileRepository{

    private val prefs = PrefManager
    private val network = NetworkManager.api

    override fun getProfile(): LiveData<User?> = prefs.provileLive

    suspend fun uploadAvatar(body: MultipartBody.Part) {
        val(url) = network.upload(body, prefs.accessToken!!)
        prefs.profile = prefs.profile!!.copy(avatar = url)
    }

    override suspend fun removeAvatar() {
        prefs.removeAvatar()
    }

    override suspend fun editProfile(name: String, about: String) {
        prefs.editProfile(name,about)
    }

}
