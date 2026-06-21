package com.man_behind.checkmate.data.local.db.entity

import androidx.room3.Embedded
import androidx.room3.Relation

data class QuestionItemWithOptions(
    @Embedded val item: QuestionSetItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "itemId"
    )
    val options: List<QuestionSetItemOptionEntity>
)

data class QuestionSectionWithItems(
    @Embedded val section: QuestionSetSectionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sectionId",
        entity = QuestionSetItemEntity::class
    )
    val items: List<QuestionItemWithOptions>
)

data class QuestionSetWithDetails(
    @Embedded val questionSet: QuestionSetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "questionSetId",
        entity = QuestionSetSectionEntity::class,
    )
    val sections: List<QuestionSectionWithItems>
)

