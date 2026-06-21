package com.man_behind.checkmate.data.local.db.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "question_items",
    foreignKeys = [
        ForeignKey(
            entity = QuestionSetSectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sectionId")]
)
data class QuestionSetItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sectionId: Long,

    val position: Int,
    val question: String,
    val guidelines: String? = null,
    val fromDocumentation: Boolean,
    val onInspection: Boolean,
)
