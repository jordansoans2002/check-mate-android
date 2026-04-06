package com.man_behind.checkmate.data.model

import java.time.LocalDateTime

data class ChecklistOverview(
    val id: Long,
    val name: String,
    val progress: List<Float>,
    val createdOn: String,
    val lastModifiedOn: String? = null,
)
