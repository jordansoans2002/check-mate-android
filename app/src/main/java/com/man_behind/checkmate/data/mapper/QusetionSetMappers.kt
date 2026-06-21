package com.man_behind.checkmate.data.mapper

import com.man_behind.checkmate.data.local.db.entity.ChecklistItemEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistItemOptionEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistSectionEntity
import com.man_behind.checkmate.data.local.db.entity.QuestionSetItemEntity
import com.man_behind.checkmate.data.local.db.entity.QuestionSetItemOptionEntity
import com.man_behind.checkmate.data.local.db.entity.QuestionSetSectionEntity

fun QuestionSetSectionEntity.toChecklistSectionEntity(checklistId: Long): ChecklistSectionEntity {
    return ChecklistSectionEntity(
        checklistId = checklistId,
        name = name,
        position = position,
        comments = "",
        lastModifiedItemId = null
    )
}

fun QuestionSetItemEntity.toChecklistItemEntity(checklistSectionId: Long): ChecklistItemEntity {
    return ChecklistItemEntity(
        sectionId = checklistSectionId,
        position = position,
        question = question,
        guidelines = guidelines,
        selectedOptionId = null,
        actionTaken = "",
        comment = "",
        fromDocumentation = fromDocumentation,
        onInspection = onInspection
    )
}

fun QuestionSetItemOptionEntity.toChecklistItemOptionEntity(checklistItemId: Long): ChecklistItemOptionEntity {
    return ChecklistItemOptionEntity(
        itemId = checklistItemId,
        position = position,
        text = text
    )
}