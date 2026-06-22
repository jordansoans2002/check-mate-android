package com.man_behind.checkmate.data.local.db.entity

import androidx.room3.Embedded
import androidx.room3.Relation

data class QuestionItemWithOptions(
    @Embedded val item: QuestionSetItemEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["itemId"]
    )
    val options: List<QuestionSetItemOptionEntity>
)

data class QuestionSectionWithItems(
    @Embedded val section: QuestionSetSectionEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["sectionId"],
        entity = QuestionSetItemEntity::class
    )
    val items: List<QuestionItemWithOptions>
)

data class QuestionSetWithDetails(
    @Embedded val questionSet: QuestionSetEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["questionSetId"],
        entity = QuestionSetSectionEntity::class,
    )
    val sections: List<QuestionSectionWithItems>
)

