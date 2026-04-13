package com.man_behind.checkmate.ui.screens.fill_checklist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.man_behind.checkmate.data.model.ChecklistItem
import com.man_behind.checkmate.data.repository.ChecklistRepository
import com.man_behind.checkmate.ui.navigation.FillChecklist
import com.man_behind.checkmate.utils.MediaManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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

    fun flushAll() {
        controllers.values.forEach { it.flush() }
    }
}
