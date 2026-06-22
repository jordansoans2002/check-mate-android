package com.man_behind.checkmate.data.repository

import android.content.Context
import com.man_behind.checkmate.data.local.csv.CsvParser
import com.man_behind.checkmate.data.local.db.DatabaseService
import com.man_behind.checkmate.data.local.db.entity.QuestionSetItemEntity
import com.man_behind.checkmate.data.local.db.entity.QuestionSetSectionEntity
import com.man_behind.checkmate.data.local.db.entity.QuestionSetWithDetails
import com.man_behind.checkmate.data.model.QuestionSetOverview
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuestionSetRepositoryImpl @Inject constructor(
    private val databaseService: DatabaseService,
    @ApplicationContext private val context: Context,
) : QuestionSetRepository {
    companion object {
        // CSV column indices — must match the header row exactly
        private const val COL_SECTION_NAME     = 1
        // COL_SECTION_NUMBER (0) and COL_QUESTION_NUMBER (2) are reference-only,
        // not stored in the DB. Position is derived from row order in the file.
        private const val COL_QUESTION         = 3
        private const val COL_GUIDELINES       = 4
        private const val COL_FROM_DOCS        = 5
        private const val COL_ON_INSPECTION    = 6
        private const val COL_OPTIONS          = 7
        private const val EXPECTED_COLUMNS     = 8

        private const val ASSET_PATH = "questions/risq_3_2.csv"
    }

    override suspend fun loadDefaultQuestionSetIfNeeded() {
        val dao = databaseService.questionSetDao()
        if (dao.hasAnyQuestionSets()) return

        loadFromCsv(name = "RISQ 3.2", assetPath = ASSET_PATH)
    }

    private suspend fun loadFromCsv(name: String, assetPath: String) {
        val csvText = context.assets.open(assetPath)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        val rows = CsvParser.parse(csvText)
        require(rows.size > 1) { "$assetPath is empty or contains only a header" }

        val header = rows.first()
        require(header.size == EXPECTED_COLUMNS) {
            "Expected $EXPECTED_COLUMNS columns, found ${header.size} columns" +
            " Check the CSV header matches: sectionNumber,sectionName,questionNumber," +
            "question,guidelines,fromDocumentation,onInspection,options"
        }

        val dataRows = rows.drop(1)

        val sectionMap = LinkedHashMap<String, MutableList<List<String>>>()
        dataRows.forEach { row ->
            if (row.size < EXPECTED_COLUMNS) return@forEach // skip malformed rows
            val sectionName = row[COL_SECTION_NAME].trim()
            sectionMap.getOrPut(sectionName) { mutableListOf() }.add(row)
        }

        val sectionsWithItems = sectionMap.entries.mapIndexed { sectionIndex, (sectionName, sectionRows) ->
            val section = QuestionSetSectionEntity(
                questionSetId = 0,
                position = sectionIndex,
                name = sectionName,
            )

            val items = sectionRows.mapIndexed { itemIndex, row ->
                val item = QuestionSetItemEntity(
                    sectionId = 0,
                    position = itemIndex,
                    question = row[COL_QUESTION].trim(),
                    guidelines = row[COL_GUIDELINES].trim().ifBlank { null },
                    fromDocumentation = row[COL_FROM_DOCS].trim().lowercase() == "true",
                    onInspection = row[COL_ON_INSPECTION].trim().lowercase() == "true"
                )

                val options = row.getOrNull(COL_OPTIONS)
                    ?.trim()
                    ?.split("|")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList()

                item to options
            }

            section to items
        }

        databaseService.questionSetDao().importQuestionSet(
            name = name,
            sectionsWithItems = sectionsWithItems.toMap()
        )
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