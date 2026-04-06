package com.man_behind.checkmate.data.local.db.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "checklist_item_images",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklistItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("checklistItemId")]
)
data class ChecklistItemImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val checklistItemId: Long,
    val uri: String
)
