package com.man_behind.checkmate.data.local.db.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "question_sets")
data class QuestionSetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
)
