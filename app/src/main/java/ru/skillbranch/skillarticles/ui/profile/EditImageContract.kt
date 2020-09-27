package ru.skillbranch.skillarticles.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract

class EditImageContract : ActivityResultContract<Pair<Uri, Uri> , Uri?>(){

    private val TAG = EditImageContract::class.java.name

    override fun createIntent(context: Context, input: Pair<Uri, Uri>?): Intent {
        val intent =  Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(input!!.first,"image/jpeg")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // без этого флага внешнее приложение не получит доступ к uri
            putExtra(MediaStore.EXTRA_OUTPUT, input.second)
            putExtra("return-value", true)
        }

        // нужно определить кому мы выдаем разрешение на запись
        // scan all application on device for resolve this intent action (action Intent.ACTION_EDIT) type
        val resolveInfoList = context.packageManager
            .queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY)
            .map { info -> info.activityInfo.packageName }

        resolveInfoList.forEach { resolvePackage ->
                context.grantUriPermission(
                    resolvePackage,
                    input!!.second,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                ) }

        Log.e(TAG,"activities (aoolication) for edit image: $resolveInfoList")

//        return intent
        return Intent.createChooser(intent,"Choose application for edit avatar")
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if(resultCode == Activity.RESULT_OK) intent?.data
        else null
    }
}