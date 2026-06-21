package com.man_behind.checkmate.data.model


data class ChecklistOverview(
    val id: Long,
    val questionSetId: Long?,

    val name: String,
    val progress: List<Float>,
    val createdOn: String,
    val lastModifiedOn: String? = null,
)
