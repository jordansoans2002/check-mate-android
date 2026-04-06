package com.man_behind.checkmate.ui.screens.all_checklists

import androidx.lifecycle.ViewModel
import com.man_behind.checkmate.data.repository.ChecklistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AllChecklistsViewModel @Inject constructor(
    private val checklistRepository: ChecklistRepository
): ViewModel() {

    private val _state = MutableStateFlow(AllChecklistsUiState())
    val state: StateFlow<AllChecklistsUiState> = _state.asStateFlow()

}