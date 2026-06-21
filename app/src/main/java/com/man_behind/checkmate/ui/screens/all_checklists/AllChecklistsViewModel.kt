package com.man_behind.checkmate.ui.screens.all_checklists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.man_behind.checkmate.data.repository.ChecklistRepository
import com.man_behind.checkmate.data.repository.QuestionSetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllChecklistsViewModel @Inject constructor(
    private val checklistRepository: ChecklistRepository,
    private val questionSetRepository: QuestionSetRepository,
): ViewModel() {

    private val _state = MutableStateFlow(AllChecklistsUiState())
    val state: StateFlow<AllChecklistsUiState> =
        combine(
            _state,
            checklistRepository.getAllChecklists(),
            questionSetRepository.getAllQuestionSets(),
        ) { state, checklists, questionSets ->
            state.copy(
                checklists = checklists,
                questionSets = questionSets,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            initialValue = AllChecklistsUiState(isLoading = true)
        )

    fun toggleCreateChecklistDialog(show: Boolean) {
        _state.update { it.copy(showCreateChecklistDialog = show) }
    }

    fun createChecklist(questionSetId: Long, name: String) {
        viewModelScope.launch {
            val checklistId = checklistRepository.createChecklist(questionSetId,name)
            _state.update { it.copy(
                newChecklistId = checklistId,
                showCreateChecklistDialog = false,
            ) }
        }
    }
}