package com.man_behind.checkmate.data.model

data class ChecklistSection(
    val id: Long,
    val name: String,
    val items: List<ChecklistItem>,
    val comments: String = "",
    val progress: Float = 0f,
    val lastModifiedItemId: Long? = null,
)
