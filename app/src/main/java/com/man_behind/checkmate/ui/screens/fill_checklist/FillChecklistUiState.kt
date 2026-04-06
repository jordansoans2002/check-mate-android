package com.man_behind.checkmate.ui.screens.fill_checklist

import com.man_behind.checkmate.data.model.Checklist

data class FillChecklistUiState(
    val checklist: Checklist? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
