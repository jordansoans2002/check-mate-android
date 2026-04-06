package com.man_behind.checkmate.data.local.db.dao

import androidx.room3.Query

abstract interface ChecklistDao {
    @Query("""
        UPDATE checklist_items
        SET 
            selectedOptionId = COALESCE(:selectedOptionId, selectedOptionId),
            actionTaken = COALESCE(:actionTaken, actionTaken),
            comment = COALESCE(:comment, comment)
        WHERE id = :itemId
    """)
    suspend fun updateChecklistItem(
        itemId: Long,
        selectedOptionId: Long?,
        actionTaken: String?,
        comment: String?
    )
}