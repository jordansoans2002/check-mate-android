package com.man_behind.checkmate.data.repository

import com.man_behind.checkmate.data.local.db.DatabaseService
import com.man_behind.checkmate.data.local.db.entity.QuestionSetItemEntity
import com.man_behind.checkmate.data.local.db.entity.QuestionSetSectionEntity
import com.man_behind.checkmate.data.local.db.entity.QuestionSetWithDetails
import com.man_behind.checkmate.data.model.QuestionSetOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuestionSetRepositoryImpl @Inject constructor(
    private val databaseService: DatabaseService
) : QuestionSetRepository {

    override suspend fun loadDefaultQuestionSetIfNeeded() {
        val dao = databaseService.questionSetDao()
        if (dao.hasAnyQuestionSets()) return

        // Dummy data for now — same shape as the eventual CSV import.
        // Replace this block with CSV parsing + dao.importQuestionSet(...) later;
        // the call site and downstream behavior won't need to change.
        val data = mapOf(
            QuestionSetSectionEntity(questionSetId = 0, position = 0, name = "General Information") to listOf(
                QuestionSetItemEntity(
                    sectionId = 0, position = 0,
                    question = "Vessel's name as it appears on the Certificate of xxx",
                    guidelines = "",
                    fromDocumentation = true, onInspection = false
                ) to emptyList(),
                QuestionSetItemEntity(
                    sectionId = 0, position = 1,
                    question = "Date the vessel was delivered",
                    guidelines = "Date of delivery can be found either in form A of the International Oil Pollution Prevention(IOPP) Certificate or Safety Construction Certificate",
                    fromDocumentation = true, onInspection = false
                ) to emptyList(),
                QuestionSetItemEntity(
                    sectionId = 0, position = 2,
                    question = "Hull Type",
                    guidelines = "",
                    fromDocumentation = true, onInspection = false
                ) to listOf("Double Bottom-Single Skin Side", "Double Hull")
            ),

            QuestionSetSectionEntity(questionSetId = 0, position = 1, name = "Certification and Personnel Management") to listOf(
                QuestionSetItemEntity(
                    sectionId = 0, position = 0,
                    question = "Has the vessel been provided with certificates of financial security for seafarers",
                    guidelines = "Check the needle is in the green zone.",
                    fromDocumentation = true, onInspection = true
                ) to listOf("Yes", "No", "N/A", "N/V"),
                QuestionSetItemEntity(
                    sectionId = 0, position = 1,
                    question = "Is the officer matrix accurately completed and does it reflect the information on officers and engineers on board the vessel at the time of inspection",
                    guidelines = "Check all 4 main exits.",
                    fromDocumentation = false, onInspection = true
                ) to listOf("Yes", "No", "N/A", "N/V")
            ),

            QuestionSetSectionEntity(questionSetId = 0, position = 2, name = "Navigation") to listOf(
                QuestionSetItemEntity(
                    sectionId = 0, position = 0,
                    question = "Minimum staff count met?",
                    guidelines = "Compare roster against floor count.",
                    fromDocumentation = true, onInspection = true
                ) to listOf("Yes", "No")
            )
        )

        dao.importQuestionSet(name = "Default", sectionsWithItems = data)
    }

    override fun getAllQuestionSets(): Flow<List<QuestionSetOverview>> =
        databaseService.questionSetDao().getAllQuestionSetsOverview().map { sets ->
            sets.map { QuestionSetOverview(id = it.id, name = it.name) }
        }

    override fun getQuestionSetById(id: Long): Flow<QuestionSetWithDetails?> =
        databaseService.questionSetDao().getQuestionSetById(id)

    override suspend fun createQuestionSet(name: String): Long {
        TODO("Not yet implemented")
    }
}