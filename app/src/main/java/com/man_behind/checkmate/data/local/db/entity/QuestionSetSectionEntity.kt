package com.man_behind.checkmate.data.local.db.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "question_sections",
    foreignKeys = [
        ForeignKey(
            entity = QuestionSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionSetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("questionSetId")]
)
data class QuestionSetSectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questionSetId: Long,

    val name: String,
    val position: Int,
)