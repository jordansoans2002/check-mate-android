package com.man_behind.checkmate.data.repository

import android.net.Uri
import com.man_behind.checkmate.data.model.Checklist
import com.man_behind.checkmate.data.model.ChecklistOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ChecklistRepositoryMockImpl : ChecklistRepository {
    override suspend fun createChecklist(name: String) {}

    override fun getAllChecklists(): Flow<List<ChecklistOverview>> = flowOf(emptyList())

    override fun getChecklistById(id: Long): Flow<Checklist?> = flowOf(null)

    override suspend fun updateChecklistItem(
        itemId: Long,
        selectedOptionId: Long?,
        actionTaken: String?,
        comment: String?,
    ) {}

    override suspend fun updateSelectedOption(itemId: Long, optionId: Long?) {}

    override suspend fun updateActionTaken(itemId: Long, actionTaken: String) {}

    override suspend fun updateComment(itemId: Long, comment: String) {}

    override suspend fun addImages(itemId: Long, images: List<Uri>) {}
    override suspend fun removeImage(itemImageId: Long) {}

    override suspend fun renameChecklist(checklistId: Long, newName: String) {}
    override suspend fun deleteChecklist(id: Long) {}
}
