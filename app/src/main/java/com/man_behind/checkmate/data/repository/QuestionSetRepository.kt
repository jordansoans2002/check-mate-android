package com.man_behind.checkmate.data.repository

import com.man_behind.checkmate.data.local.db.entity.QuestionSetWithDetails
import com.man_behind.checkmate.data.model.QuestionSetOverview
import kotlinx.coroutines.flow.Flow

interface QuestionSetRepository {
    /**
     * Loads the fixed/bundled question set into the DB if there are no
     * question sets at all yet. Safe to call on every app start — it's a
     * no-op after the first successful load. Call this once during app
     * startup, before the question-set picker / checklist creation can be reached.
     */
    suspend fun loadDefaultQuestionSetIfNeeded()

    fun getAllQuestionSets(): Flow<List<QuestionSetOverview>>

    suspend fun createQuestionSet(name: String): Long

    fun getQuestionSetById(id: Long): Flow<QuestionSetWithDetails?>

}