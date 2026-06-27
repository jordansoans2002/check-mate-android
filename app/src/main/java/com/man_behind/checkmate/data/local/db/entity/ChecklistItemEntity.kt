package com.man_behind.checkmate.data.local.db.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistSectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChecklistItemOptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["selectedOptionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sectionId"),
        Index("selectedOptionId")
    ]
)
data class ChecklistItemEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sectionId: Long,

    val position: Int,
    val question: String,
    val guidelines: String? = null,
    val selectedOptionId: Long? = null,
    val actionTaken: String = "",
    val comment: String = "",
    val fromDocumentation: Boolean,
    val onInspection: Boolean,
)
