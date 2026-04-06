package com.man_behind.checkmate.data.model

import java.time.LocalDateTime

data class Checklist(
    val id: Long = 0,
    val name: String,
    val sections: List<ChecklistSection>,
    val comments: String = "",
    val createdOn: LocalDateTime,
    val lastModifiedSectionId: Long? = null,
    val lastModifiedOn: LocalDateTime? = null,
)
