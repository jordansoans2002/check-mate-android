package com.man_behind.checkmate.data.model

import android.net.Uri

data class ChecklistItem(
    val id: Long,
    val question: String,
    val guidelines: String? = null,
    val options: List<ChecklistItemOption>,
    val selectedOptionId: Long? = null,
    val actionTaken: String = "",
    val comment: String = "",
    val fromDocumentation: Boolean,
    val onInspection: Boolean,
    val position: Int,
    val images: List<ChecklistItemImages> = emptyList()
)
