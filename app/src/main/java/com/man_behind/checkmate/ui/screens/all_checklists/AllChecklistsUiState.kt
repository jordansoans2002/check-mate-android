package com.man_behind.checkmate.ui.screens.all_checklists

import com.man_behind.checkmate.data.model.ChecklistOverview
import com.man_behind.checkmate.data.model.QuestionSetOverview
import java.io.File

data class AllChecklistsUiState(
    val checklists: List<ChecklistOverview> = emptyList(),
    val questionSets: List<QuestionSetOverview> = emptyList(),
    val showCreateChecklistDialog: Boolean = false,
    val newChecklistId: Long? = null,
    val selectedChecklists: Set<Long> = emptySet(),
    val tempFiles: List<File>? = null,
    val isLoading: Boolean = false,
    val toast: String? = null
)
