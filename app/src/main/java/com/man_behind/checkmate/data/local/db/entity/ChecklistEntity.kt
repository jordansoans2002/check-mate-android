package com.man_behind.checkmate.data.local.db.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "checklists",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistSectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["lastModifiedSectionId"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ]
)
data class ChecklistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val comments: String = "",
    val createdOn: LocalDateTime,
    val lastModifiedSectionId: Long? = null,
    val lastModifiedOn: LocalDateTime? = null
)
