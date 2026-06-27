package com.man_behind.checkmate.ui.screens.all_checklists

import com.man_behind.checkmate.data.model.ChecklistOverview
import com.man_behind.checkmate.data.model.QuestionSetOverview

data class AllChecklistsUiState(
    val checklists: List<ChecklistOverview> = emptyList(),
    val questionSets: List<QuestionSetOverview> = emptyList(),
    val showCreateChecklistDialog: Boolean = false,
    val selectedChecklists: Set<Long> = emptySet(),
    val showSaveExportDialog: Boolean = false,
    val isLoading: Boolean = false,
    val toast: String? = null
)
