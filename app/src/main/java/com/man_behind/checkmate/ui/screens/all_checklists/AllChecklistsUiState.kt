package com.man_behind.checkmate.ui.screens.all_checklists

import com.man_behind.checkmate.data.model.ChecklistOverview

data class AllChecklistsUiState(
    val checklists: List<ChecklistOverview> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
