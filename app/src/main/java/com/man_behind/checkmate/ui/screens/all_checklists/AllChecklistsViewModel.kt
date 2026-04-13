package com.man_behind.checkmate.ui.screens.all_checklists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.man_behind.checkmate.data.repository.ChecklistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllChecklistsViewModel @Inject constructor(
    private val checklistRepository: ChecklistRepository
): ViewModel() {

    val state: StateFlow<AllChecklistsUiState> = checklistRepository
        .getAllChecklists()
        .map { checkLists ->
            AllChecklistsUiState(
                checklists = checkLists,
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            initialValue = AllChecklistsUiState(isLoading = true)
        )

    fun createChecklist() {
        viewModelScope.launch {
            checklistRepository.createChecklist("Test Checklist")
        }
    }
}