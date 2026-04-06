package com.man_behind.checkmate.data.repository

import android.net.Uri
import com.man_behind.checkmate.data.model.Checklist
import com.man_behind.checkmate.data.model.ChecklistOverview
import kotlinx.coroutines.flow.Flow

class ChecklistRepositoryImpl: ChecklistRepository {
    override suspend fun createChecklist(name: String) {
        TODO("Not yet implemented")
    }

    override fun getAllChecklists(): Flow<List<ChecklistOverview>> {
        TODO("Not yet implemented")
    }

    override fun getChecklistById(id: Long): Flow<Checklist> {
        TODO("Not yet implemented")
    }

    override suspend fun updateChecklistItem(
        itemId: Long,
        selectedOptionId: Long?,
        actionTaken: String?,
        comment: String?,
        images: List<Uri>?
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun updateSelectedOption(itemId: Long, optionId: Long?) {
        TODO("Not yet implemented")
    }

    override suspend fun updateImages(
        itemId: Long,
        images: List<Uri>
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun updateActionTaken(itemId: Long, actionTaken: String) {
        TODO("Not yet implemented")
    }

    override suspend fun updateComment(itemId: Long, comment: String) {
        TODO("Not yet implemented")
    }

    override suspend fun renameChecklist(checklistId: Long, newName: String) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteChecklist(id: Long) {
        TODO("Not yet implemented")
    }
}