package com.man_behind.checkmate.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getTempCameraUri(): Uri {
        val directory = File(context.externalCacheDir, "images").apply { mkdirs() }
        val file = File.createTempFile("captured_image_", ".jpg", directory)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun saveToGallery(uri: Uri): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "CheckMate_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CheckMate")
        }

        val targetUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        targetUri?.let { dest ->
            resolver.openOutputStream(dest)?.use { out ->
                resolver.openInputStream(uri)?.use { input ->
                    input.copyTo(out)
                }
            }
        }
        targetUri
    }

    suspend fun saveMultipleToGallery(uris: List<Uri>): List<Uri> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            saveToGallery(uri)
        }
    }

}