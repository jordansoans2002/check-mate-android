package com.man_behind.checkmate.data.local.db.entity

import androidx.room3.Embedded
import androidx.room3.Relation

data class ChecklistItemWithDetails(
    @Embedded val item: ChecklistItemEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["itemId"]
    )
    val options: List<ChecklistItemOptionEntity>,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["itemId"]
    )
    val images: List<ChecklistItemImageEntity>
)

data class ChecklistSectionWithItems(
    @Embedded val section: ChecklistSectionEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["sectionId"],
        entity = ChecklistItemEntity::class
    )
    val items: List<ChecklistItemWithDetails>
)

data class ChecklistWithDetails(
    @Embedded val checklist: ChecklistEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["checklistId"],
        entity = ChecklistSectionEntity::class
    )
    val sections: List<ChecklistSectionWithItems>
)
