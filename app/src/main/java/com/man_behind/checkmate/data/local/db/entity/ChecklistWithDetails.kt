package com.man_behind.checkmate.data.local.db.entity

import androidx.room3.Embedded
import androidx.room3.Relation

data class ChecklistItemWithDetails(
    @Embedded val item: ChecklistItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "itemId"
    )
    val options: List<ChecklistItemOptionEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "itemId"
    )
    val images: List<ChecklistItemImageEntity>
)

data class ChecklistSectionWithItems(
    @Embedded val section: ChecklistSectionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sectionId",
        entity = ChecklistItemEntity::class
    )
    val items: List<ChecklistItemWithDetails>
)

data class ChecklistWithDetails(
    @Embedded val checklist: ChecklistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "checklistId",
        entity = ChecklistSectionEntity::class
    )
    val sections: List<ChecklistSectionWithItems>
)
