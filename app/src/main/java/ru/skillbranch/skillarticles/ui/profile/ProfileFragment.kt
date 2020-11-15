package ru.skillbranch.skillarticles.ui.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistryOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.ui.RootActivity
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.ui.base.Binding
import ru.skillbranch.skillarticles.ui.custom.delegates.RenderProp
import ru.skillbranch.skillarticles.ui.dialogs.AvatarActionsDialog
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand
import ru.skillbranch.skillarticles.viewmodels.profile.PendingAction
import ru.skillbranch.skillarticles.viewmodels.profile.ProfileState
import ru.skillbranch.skillarticles.viewmodels.profile.ProfileViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment() : BaseFragment<ProfileViewModel>() {

    private lateinit var resultRegistry: ActivityResultRegistry
    var _mockFactory: ((SavedStateRegistryOwner) -> ViewModelProvider.Factory)? = null

    override val viewModel: ProfileViewModel by viewModels {
        _mockFactory?.invoke(this) ?: defaultViewModelProviderFactory
    }

    private val TAG = ProfileFragment::class.java.name

 //   override val viewModel: ProfileViewModel by viewModels()
    override val layout: Int = R.layout.fragment_profile

    override val binding: ProfileBinding by lazy {
        ProfileBinding()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    constructor(
        mockRoot: RootActivity,
        testRegistry: ActivityResultRegistry? = null,
        mockFactory: ((SavedStateRegistryOwner) -> ViewModelProvider.Factory)? = null
    ): this(){
        _mockRoot = mockRoot
        _mockFactory = mockFactory
        if(testRegistry != null) resultRegistry = testRegistry
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var permissionsLauncher: ActivityResultLauncher<Array<out String>>
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var galleryLauncher: ActivityResultLauncher<String>
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var editPhotoLauncher: ActivityResultLauncher<Pair<Uri,Uri>>
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var settingsLauncher: ActivityResultLauncher<Intent>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(!::resultRegistry.isInitialized) resultRegistry = requireActivity().activityResultRegistry

        permissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions(),resultRegistry,::callbackPermissions)
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture(),resultRegistry,::callbackCamera)
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent(),resultRegistry,::callbackGallery)
        editPhotoLauncher = registerForActivityResult(EditImageContract(),resultRegistry,::callbackEditPhoto)
        settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),resultRegistry,::callbackSettings)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // listen fragment result
        setFragmentResultListener(AvatarActionsDialog.AVATAR_ACTIONS_KEY){
                requestKey, bundle ->
            when(bundle[AvatarActionsDialog.SELECTED_ACTION_KEY] as String){

                AvatarActionsDialog.CAMERA_KEY -> viewModel.handleCameraAction(prepareTempUri())

                AvatarActionsDialog.GALLERY_KEY -> viewModel.handleGalleryAction()

                AvatarActionsDialog.DELETE_KEY -> {
                    viewModel.handleDeleteAction()
                }

                AvatarActionsDialog.EDIT_KEY -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val sourceFile = Glide.with(requireActivity())
                            .asFile()
                            .load(binding.avatar)
                            .submit()
                            .get()

                        val sourceUri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.provider",
                            sourceFile
                        )

                        Log.e(TAG, "edit image cache uri: ${sourceFile.toURI()} content sourceUri: $sourceUri")

                        withContext(Dispatchers.Main){
                            viewModel.handleEditAction(sourceUri,prepareTempUri())
                        }
                    }
                }
            }
        }
    }

    private fun callbackPermissions(result: Map<String, Boolean>){
        val permissinsResult = result.mapValues {
            (permission, isGranted) ->
            if(isGranted) true to true
            else false to ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                permission
            )
        }
        val isAllGranted = !permissinsResult.values.map { it.first }.contains(false)
        if(!isAllGranted){
            val tempUri = when(val pendingAction = binding.pendingAction){
                is PendingAction.CameraAction -> pendingAction.payload
                is PendingAction.EditAction -> pendingAction.payload.second
                else -> null
            }
            removeTempUri(tempUri)
        }
        viewModel.handlePermission(permissinsResult)
    }

//    private val callbackPermissions =
//        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
//            result ->
//            Log.e(TAG,"Request runtime permission result: ${result}")
//            val permissionResult = result.mapValues { (permission: String,isGranted: Boolean) ->
//               if(isGranted) true to true
//                else false to ActivityCompat.shouldShowRequestPermissionRationale(
//                   requireActivity(),
//                   permission
//               )
//            }
//            viewModel.handlePermission(permissionResult)
//        }

    private fun callbackCamera(result: Boolean){
        val(payload) = binding.pendingAction as PendingAction.CameraAction
        if(result){
            val inputStream = requireContext().contentResolver.openInputStream(payload)
            viewModel.handleUploadPhoto(inputStream)
        }else{
            removeTempUri(payload)
        }
    }

    private fun callbackSettings(result: ActivityResult) {
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // после страничек настройки передаем управление сюда и здесь можем
            // сделать что то еще
            Log.e(TAG, " return from registerForActivityResult")
        }
    }
//    private val settingsResultCallback =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
//            // после страничек настройки передаем управление сюда и здесь можем
//            // сделать что то еще
//            Log.e(TAG," return from registerForActivityResult")
//        }

    private fun callbackGallery(result: Uri?){
        if(result != null){
            val inputStream = requireContext().contentResolver.openInputStream(result)
            viewModel.handleUploadPhoto(inputStream)
        }
    }
//    private val galleryResultCallback =
//        registerForActivityResult(ActivityResultContracts.GetContent()){uri ->
//
//        if(uri != null){
//            val inputStream = requireContext().contentResolver.openInputStream(uri)
//            viewModel.handleUploadPhoto(inputStream)
//        }
//    }

    private fun callbackEditPhoto(result: Uri?){
        if(result != null){
            // фото было сделано
            // получаем contentResolver и по uri получаем inputStream
            val inputStream = requireContext().contentResolver.openInputStream(result)
            viewModel.handleUploadPhoto(inputStream)
        }else{
            val(payload) = binding.pendingAction as PendingAction.EditAction
            removeTempUri(payload.second)
        }
    }
//    private val cameraResultCallback = registerForActivityResult(ActivityResultContracts.TakePicture())
//    { result ->
//        val(payload) = binding.pendingAction as PendingAction.CameraAction
//
//        if(result){
//            // фото было сделано
//            // получаем contentResolver и по uri получаем inputStream
//            val inputStream = requireContext().contentResolver.openInputStream(payload)
//            viewModel.handleUploadPhoto(inputStream)
//        }else{
//            removeTempUri(payload)
//        }
//    }

//    private val editPhotoResultCallback =
//        registerForActivityResult(EditImageContract()){
//        result ->
//            if(result != null){
//                val inputStream = requireContext().contentResolver.openInputStream(result)
//                viewModel.handleUploadPhoto(inputStream)
//            }else{
//                val(payload) = binding.pendingAction as PendingAction.EditAction
//                removeTempUri(payload.second)
//            }
//    }


    override fun setupViews() {
        iv_avatar.setOnClickListener {
            val action = ProfileFragmentDirections.actionNavProfileToDialogAvatarActions(binding.avatar.isNotBlank())
            viewModel.navigate(NavigationCommand.To(action.actionId, action.arguments))
          }

        binding.bind(viewModel.currentState)

        viewModel.observerPermissions(viewLifecycleOwner) {
                       permissionsLauncher.launch(it.toTypedArray())
            //           callbackPermissions.launch(it.toTypedArray())
        }

        viewModel.observeActivityResults(viewLifecycleOwner) {
            when (it) {
                is PendingAction.GalleryAction -> {
//                    galleryResultCallback.launch(it.payload)
                    galleryLauncher.launch(it.payload)
                }
                is PendingAction.SettingsAction -> {
                    settingsLauncher.launch(it.payload)
//                    settingsResultCallback.launch(it.payload)
                }
                is PendingAction.CameraAction ->{
                    cameraLauncher.launch(it.payload)
//                    cameraResultCallback.launch(it.payload)
                }
                is PendingAction.EditAction -> {
                    editPhotoLauncher.launch(it.payload)
//                    editPhotoResultCallback.launch(it.payload)
                }
            }
        }
    }

    /**
     * вернем contentUri а не просто имя файла
     * это нужно для передачи файлика в сторонние приложения
     * на андроид больше 7 при попытке передать в другое приложение просто файл
     * будет runtime Error
     * дописываем в Manifest блок Provider
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun prepareTempUri(): Uri {
        val timeStamp = SimpleDateFormat("HHmmss").format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val tempFile = File.createTempFile("JPEG_${timeStamp}",".jpg",storageDir)

        val contentUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            tempFile
        )
        Log.e(TAG,"file uri: ${tempFile.toUri()} content uri: $contentUri")

        return  contentUri
    }

    private fun updateAvatar(avatarUrl: String) {
        if(avatarUrl.isBlank()){
            Glide.with(this)
                .load(R.drawable.ic_avatar)
                .into(iv_avatar)
        }else{
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_avatar)
                .apply (RequestOptions.circleCropTransform())
                .into(iv_avatar)
        }
    }

    @VisibleForTesting
    fun removeTempUri(uri: Uri?) {
        uri ?: return
        requireContext().contentResolver.delete(uri,null,null)
    }

    inner class ProfileBinding() : Binding() {

        var pendingAction: PendingAction? = null

        var avatar by RenderProp("") {
            updateAvatar(it)
        }

        var name by RenderProp("") {
            tv_name.text = it
        }

        var about by RenderProp("") {
            tv_name.text = it
        }

        var rating by RenderProp(0) {
            tv_rating.text = "Rating: $it"
        }

        var respect by RenderProp(0) {
            tv_respect.text = "Respect: $it"
        }

        override fun bind(data: IViewModelState) {
            data as ProfileState
            if(data.avatar !=null) avatar = data.avatar
            if(data.about != null) about = data.about
            if(data.name != null) name = data.name
            respect = data.respect
            rating = data.rating
            pendingAction = data.pendingAction
        }

    }

}
