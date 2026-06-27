package com.man_behind.checkmate.data.model

data class ChecklistSection(
    val id: Long,
    val name: String,
    val items: List<ChecklistItem>,
    val comments: String = "",

    val position: Int,
    val lastModifiedItemId: Long? = null,
)
