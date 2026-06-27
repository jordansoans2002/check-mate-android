package com.man_behind.checkmate.data.repository

import android.net.Uri
import com.man_behind.checkmate.data.model.Checklist
import com.man_behind.checkmate.data.model.ChecklistOverview
import kotlinx.coroutines.flow.Flow

interface ChecklistRepository {
    suspend fun createChecklist(questionSetId: Long, name: String): Long
    fun getAllChecklists(): Flow<List<ChecklistOverview>>

    fun getChecklistById(id: Long): Flow<Checklist?>
    suspend fun updateChecklistItem(
        itemId: Long,
        selectedOptionId: Long? = null,
        actionTaken: String? = null,
        comment: String? = null,
    )
    suspend fun updateSelectedOption(
        itemId: Long,
        optionId: Long?,
    )
    suspend fun updateActionTaken(
        itemId: Long,
        actionTaken: String
    )
    suspend fun updateComment(
        itemId: Long,
        comment: String
    )
    suspend fun addImages(
        itemId: Long,
        images: List<Uri>
    )

    suspend fun removeImage(itemId: Long, itemImageId: Long)

    suspend fun renameChecklist(
        checklistId: Long,
        newName: String
    )

    suspend fun getChecklistImageUris(checklistIds: List<Long>): List<Uri>
    suspend fun deleteChecklists(ids: List<Long>)

}