package com.man_behind.checkmate.utils

import android.content.ContentValues
import android.util.Log
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

    fun getTempCameraUri(): Uri? {
        return try {
            val cacheDirectory = context.externalCacheDir ?: context.cacheDir
            val directory = File(cacheDirectory, "images").apply { mkdirs() }
            val file = File.createTempFile("captured_image_", ".jpg", directory)
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e("MediaManager", "Failed to create temp file: $e")
            null
        }
    }

    fun deleteTempFile(uri: Uri) {
        // Safe to call even if the URI is not a temp file — it will just no-op
        // since FileProvider URIs resolve to externalCacheDir which we own
        val path = uri.path ?: return
        val file = File(path)
        if (file.exists() && file.canonicalPath.startsWith(
                context.externalCacheDir?.canonicalPath ?: return
            )
        ) {
            file.delete()
        }
    }

    /**
     * Saves an image to internal app storage (filesDir/checklists/{checklistId}/images/).
     * Tied to the app — deleted on uninstall. Use this for attaching images to checklist items.
     * Returns a file:// URI, or null on failure.
     */
    suspend fun saveToAppStorage(uri: Uri, checklistId: Long): Uri? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "checklists/$checklistId/images").apply { mkdirs() }
            val dest = File(dir, "img_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
            Uri.fromFile(dest)
        } catch (e: Exception) {
            Log.e("MediaManager", "Failed to save to app storage: $e")
            null
        }
    }

    suspend fun saveMultipleToAppStorage(uris: List<Uri>, checklistId: Long): List<Uri> =
        withContext(Dispatchers.IO) {
            uris.mapNotNull { saveToAppStorage(it, checklistId) }
        }

    /**
     * Deletes an image saved to app storage.
     * Safe to call with any URI — gallery URIs (content://) are ignored,
     * only file:// URIs within app storage are deleted.
     */
    fun deleteFromAppStorage(uri: Uri) {
        if (uri.scheme != "file") return
        val file = File(uri.path ?: return)
        if (!file.canonicalPath.startsWith(context.filesDir.canonicalPath)) return
        if (file.exists()) file.delete()
    }

    /**
     * Saves an image to the public gallery (Pictures/CheckMate).
     * Survives app uninstall. Visible externally in the Photos app and file managers.
     * Returns the MediaStore URI of the saved image, or null on failure.
     */
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
        uris.mapNotNull { saveToGallery(it) }
    }

}