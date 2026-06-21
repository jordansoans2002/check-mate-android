package com.man_behind.checkmate.data.local.db.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "checklist_sections",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChecklistItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["lastModifiedItemId"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ],
    indices = [Index("checklistId")]
)
data class ChecklistSectionEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val checklistId: Long,

    val name: String,
    val comments: String = "",

    val position: Int,
    val lastModifiedItemId: Long? = null
)