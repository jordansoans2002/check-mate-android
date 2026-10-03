package com.man_behind.checkmate.data.repository

import android.net.Uri
import androidx.core.net.toUri
import com.man_behind.checkmate.data.local.db.DatabaseService
import com.man_behind.checkmate.data.local.db.entity.ChecklistItemImageEntity
import com.man_behind.checkmate.data.mapper.toModel
import com.man_behind.checkmate.data.model.Checklist
import com.man_behind.checkmate.data.model.ChecklistOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ChecklistRepositoryImpl @Inject constructor(
    private val databaseService: DatabaseService,
): ChecklistRepository {

    override suspend fun createChecklist(questionSetId: Long, name: String): Long =
        databaseService.checklistDao().createChecklist(questionSetId, name)


    override fun getAllChecklists(): Flow<List<ChecklistOverview>> {
        val formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy")

        return databaseService.checklistDao().getAllChecklistsOverview().map { map ->
            map.map { (entity, progressList) ->
                ChecklistOverview(
                    id = entity.id,
                    questionSetId = entity.questionSetId,
                    name = entity.name,
                    progress = progressList,
                    createdOn = entity.createdOn.format(formatter),
                    lastModifiedOn = entity.lastModifiedOn?.format(formatter),
                )
            }
        }
    }

    override fun getChecklistById(id: Long): Flow<Checklist?> =
        databaseService.checklistDao().getChecklistById(id).map { it?.toModel() }

    override suspend fun updateSelectedOption(itemId: Long, optionId: Long?) {
        databaseService.checklistDao().updateSelectedOptionWithMetadata(itemId, optionId)
    }

    override suspend fun updateComment(itemId: Long, comment: String) {
        databaseService.checklistDao().updateCommentWithMetadata(itemId, comment)
    }

    override suspend fun updateActionTaken(itemId: Long, actionTaken: String) {
        databaseService.checklistDao().updateActionTakenWithMetadata(itemId, actionTaken)
    }

    override suspend fun saveCommentAction(
        itemId: Long,
        comment: String,
        actionTaken: String
    ) {
        databaseService.checklistDao().updateCommentActionWithMetadata(
            itemId = itemId,
            comment = comment,
            actionTaken = actionTaken
        )
    }

    override suspend fun addImages(itemId: Long, images: List<Uri>) {
        databaseService.checklistDao().insertImagesWithMetadata(
            itemId = itemId,
            images = images.map {
                ChecklistItemImageEntity(itemId = itemId, uri = it.toString())
            }
        )
    }

    override suspend fun removeImage(itemId: Long, itemImageId: Long) {
        databaseService.checklistDao().deleteImageWithMetadata(itemId, itemImageId)
    }


    override suspend fun renameChecklist(checklistId: Long, newName: String) {
        // No-op for UI testing
    }

    override suspend fun getChecklistImageUris(checklistIds: List<Long>): List<Uri> =
        databaseService.checklistDao().getChecklistImages(checklistIds)
            .map { it.toUri() }

    override suspend fun deleteChecklists(ids: List<Long>) {
        databaseService.checklistDao().deleteChecklists(ids)
    }
}
