package com.man_behind.checkmate.ui.screens.fill_checklist

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.man_behind.checkmate.data.model.ChecklistItem
import com.man_behind.checkmate.data.repository.ChecklistRepository
import com.man_behind.checkmate.ui.navigation.FillChecklist
import com.man_behind.checkmate.utils.MediaManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FillChecklistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val checklistRepository: ChecklistRepository,
    val mediaManager: MediaManager,
): ViewModel() {
    private val route = savedStateHandle.toRoute<FillChecklist>()
    private val checklistId = route.checklistId
    private val _state = MutableStateFlow(FillChecklistUiState())
    val state: StateFlow<FillChecklistUiState> = _state.asStateFlow()

    val snackbar = MutableSharedFlow<String>()

    val checklist = checklistRepository.getChecklistById(checklistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val controllers = mutableMapOf<Long, ChecklistItemEditController>()

    fun getController(item: ChecklistItem): ChecklistItemEditController {
        return controllers.getOrPut(item.id) {
            ChecklistItemEditController(
                itemId = item.id,
                initialAction = item.actionTaken,
                initialComment = item.comment,
                repository = checklistRepository,
                scope = viewModelScope
            )
        }
    }

    fun onCameraImageCaptured(tempUri: Uri, item: ChecklistItem) {
        // if saving to app storage + gallery
        viewModelScope.launch {
            mediaManager.saveToGallery(tempUri)
            val appUri = mediaManager.saveToAppStorage(tempUri, checklistId)
            appUri?.let { getController(item).onImagesAdded(listOf(it)) }
            mediaManager.deleteTempFile(tempUri)
        }

        // saving to gallery
//        viewModelScope.launch {
//            val galleryUri = mediaManager.saveToGallery(tempUri)
//            galleryUri?.let { getController(item).onImagesAdded(listOf(it)) }
//            mediaManager.deleteTempFile(tempUri)
//        }
    }

    fun onGalleryImagesAdded(uris: List<Uri>, item: ChecklistItem) {

        // If storing to app storage
        viewModelScope.launch {
            val appUris = mediaManager.saveMultipleToAppStorage(uris, checklistId)
            getController(item).onImagesAdded(appUris)
        }

        // Already in gallery — just store the URIs as-is
//        getController(item).onImagesAdded(uris)
    }

    fun onImageRemoved(item: ChecklistItem, itemImageId: Long, uri: Uri) {
        // deletes images only if they are in app storage
        mediaManager.deleteFromAppStorage(uri)
        getController(item).onImageRemoved(itemImageId)
    }

    fun flushAll() {
        controllers.values.forEach { it.flush() }
    }
}
