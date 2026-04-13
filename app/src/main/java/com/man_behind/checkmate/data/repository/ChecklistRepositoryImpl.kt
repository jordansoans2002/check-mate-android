package com.man_behind.checkmate.data.repository

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.man_behind.checkmate.data.local.db.DatabaseService
import com.man_behind.checkmate.data.local.db.dao.ChecklistDao
import com.man_behind.checkmate.data.model.Checklist
import com.man_behind.checkmate.data.model.ChecklistItem
import com.man_behind.checkmate.data.model.ChecklistItemOption
import com.man_behind.checkmate.data.model.ChecklistOverview
import com.man_behind.checkmate.data.model.ChecklistSection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import androidx.core.net.toUri
import com.man_behind.checkmate.data.local.db.entity.ChecklistEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistItemEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistItemImageEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistSectionEntity
import com.man_behind.checkmate.data.model.ChecklistItemImages
import kotlin.to

class ChecklistRepositoryImpl @Inject constructor(
    private val databaseService: DatabaseService,
): ChecklistRepository {
//    override suspend fun createChecklist(name: String) {
//        // No-op for UI testing
//    }

    override suspend fun createChecklist(name: String) {
        val checklist = ChecklistEntity(
            name = name,
            createdOn = LocalDateTime.now(),
            lastModifiedOn = LocalDateTime.now()
        )

        // Define Dummy Data Structure
        val data = mapOf(
            // Section 1: Documentation Heavy
            ChecklistSectionEntity(name = "General Information", checklistId = 0) to listOf(
                ChecklistItemEntity(
                    checklistId = 0, sectionId = 0, position = 1,
                    question = "Vessel's name as it appears on the Certificate of xxx",
                    guidelines = "",
                    fromDocumentation = true, onInspection = false
                ) to emptyList(),
                ChecklistItemEntity(
                    checklistId = 0, sectionId = 0, position = 2,
                    question = "Date the vessel was delivered",
                    guidelines = "Date of delivery can be found either in form A of the International Oil Pollution Prevention(IOPP) Certificate or Safety Construction Certificate",
                    fromDocumentation = true, onInspection = false
                ) to emptyList(),
                ChecklistItemEntity(
                    checklistId = 0, sectionId = 0, position = 3,
                    question = "Hull Type",
                    guidelines = "",
                    fromDocumentation = true, onInspection = false
                ) to listOf("Double Bottom-Single Skin Side", "Double Hull")
            ),

            ChecklistSectionEntity(name = "Certification and Personnel Management", checklistId = 0) to listOf(
                ChecklistItemEntity(
                    checklistId = 0, sectionId = 0, position = 1,
                    question = "Has the vessel been provided with certificates of financial security for seafarers",
                    guidelines = "Check the needle is in the green zone.",
                    fromDocumentation = true, onInspection = true
                ) to listOf("Yes", "No", "N/A", "N/V"),
                ChecklistItemEntity(
                    checklistId = 0, sectionId = 0, position = 2,
                    question = "Is the officer matrix accurately completed and does it reflect the information on officers and engineers on board the vessel at the time of inspection",
                    guidelines = "Check all 4 main exits.",
                    fromDocumentation = false, onInspection = true
                ) to listOf("Yes", "No", "N/A", "N/V"),
            ),

            // Section 3: Personnel
            ChecklistSectionEntity(name = "Navigation", checklistId = 0) to listOf(
                ChecklistItemEntity(
                    checklistId = 0, sectionId = 0, position = 1,
                    question = "Minimum staff count met?",
                    guidelines = "Compare roster against floor count.",
                    fromDocumentation = true, onInspection = true
                ) to listOf("Yes", "No")
            )
        )

        // Execute the transaction
        databaseService.checklistDao().createChecklist(checklist, data)
    }


    override fun getAllChecklists(): Flow<List<ChecklistOverview>> {
        val formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy")

        return databaseService.checklistDao().getAllChecklistsOverview().map { map ->
            map.map { (entity, progressList) ->
                ChecklistOverview(
                    id = entity.id,
                    name = entity.name,
                    progress = progressList,
                    createdOn = entity.createdOn.format(formatter),
                    lastModifiedOn = entity.lastModifiedOn?.format(formatter),
                )
            }
        }
    }

    override fun getChecklistById(id: Long): Flow<Checklist?> {
        return databaseService.checklistDao().getChecklistById(id).map { result ->
            result?.let { result ->
                Checklist(
                    id = result.checklist.id,
                    name = result.checklist.name,
                    comments = result.checklist.comments,
                    createdOn = result.checklist.createdOn,
                    lastModifiedOn = result.checklist.lastModifiedOn,
                    sections = result.sections.map { section ->
                        ChecklistSection(
                            id = section.section.id,
                            name = section.section.name,
                            comments = section.section.comments,
                            items = section.items.map { item ->
                                ChecklistItem(
                                    id = item.item.id,
                                    question = item.item.question,
                                    guidelines = item.item.guidelines,
                                    options = item.options.map { option ->
                                        ChecklistItemOption(
                                            id = option.id,
                                            text = option.text
                                        )
                                    },
                                    selectedOptionId = item.item.selectedOptionId,
                                    actionTaken = item.item.actionTaken,
                                    comment = item.item.comment,
                                    fromDocumentation = item.item.fromDocumentation,
                                    onInspection = item.item.onInspection,
                                    position = item.item.position,
                                    images = item.images.map {
                                        ChecklistItemImages(
                                            id = it.id,
                                            uri = it.uri.toUri()
                                        )
                                    },
                                )
                            }
                        )
                    }
                )
            }
        }
    }

    override suspend fun updateChecklistItem(
        itemId: Long,
        selectedOptionId: Long?,
        actionTaken: String?,
        comment: String?,
    ) {
//        databaseService.checklistDao().updateItemDetails(
//            itemId = itemId,
//            selectedOptionId = selectedOptionId,
//            actionTaken = actionTaken ?: "",
//            comment = comment ?: ""
//        )
    }

    override suspend fun updateSelectedOption(itemId: Long, optionId: Long?) {
        databaseService.checklistDao().updateSelectedOption(itemId, optionId)
    }

    override suspend fun addImages(itemId: Long, images: List<Uri>) {
        databaseService.checklistDao().insertImages(
            images.map {
                ChecklistItemImageEntity(checklistItemId = itemId, uri = it.toString())
            }
        )
    }

    override suspend fun removeImage(itemImageId: Long) {
        databaseService.checklistDao().deleteImages(itemImageId)
    }

    override suspend fun updateComment(itemId: Long, comment: String) {
        databaseService.checklistDao().updateComment(itemId, comment)
    }

    override suspend fun updateActionTaken(itemId: Long, actionTaken: String) {
        databaseService.checklistDao().updateActionTaken(itemId, actionTaken)
    }

    override suspend fun renameChecklist(checklistId: Long, newName: String) {
        // No-op for UI testing
    }

    override suspend fun deleteChecklist(id: Long) {
        // No-op for UI testing
    }
}
