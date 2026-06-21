package com.man_behind.checkmate.data.local.db.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "question_item_options",
    foreignKeys = [
        ForeignKey(
            entity = QuestionSetItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId")]
)
data class QuestionSetItemOptionEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,

    val text: String,
    val position: Int,
)