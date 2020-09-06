package ru.skillbranch.skillarticles.ui.profile

import androidx.fragment.app.viewModels
import kotlinx.android.synthetic.main.fragment_profile.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.ui.base.Binding
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.profile.ProfileState
import ru.skillbranch.skillarticles.viewmodels.profile.ProfileViewModel

class ProfileFragment : BaseFragment<ProfileViewModel>() {

    override val viewModel: ProfileViewModel by viewModels()
    override val layout: Int = R.layout.fragment_profile

    override val binding: ProfileBinding by lazy{
        ProfileBinding()
    }

    override fun setupViews() {
        binding.bind(viewModel.currentState)
    }

    private fun updateAvatar(avatarUrl: String) {

    }

    inner class ProfileBinding(): Binding(){

        var avatar by RenderProp("",true,{
            updateAvatar(it)
        })

        var name by RenderProp(""){
            tv_name.text = it
        }

        var about by RenderProp(""){
            tv_name.text = it
        }

        var rating by RenderProp(0){
            tv_rating.text = "Rating: $it"
        }

        var respect by RenderProp(0){
            tv_respect.text = "Respect: $it"
        }

        override fun bind(data: IViewModelState) {
            data as ProfileState
            avatar = data.avatar ?: ""
            about = data.about ?: ""
            name = data.name ?: ""
            respect = data.respect
            rating = data.rating

        }

    }

}
