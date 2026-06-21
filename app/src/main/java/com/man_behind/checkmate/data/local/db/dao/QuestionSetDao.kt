package com.man_behind.checkmate.data.local.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import com.man_behind.checkmate.data.local.db.entity.QuestionSetItemEntity
import com.man_behind.checkmate.data.local.db.entity.QuestionSetItemOptionEntity
import com.man_behind.checkmate.data.local.db.entity.QuestionSetSectionEntity
import com.man_behind.checkmate.data.local.db.entity.QuestionSetEntity
import com.man_behind.checkmate.data.local.db.entity.QuestionSetWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionSetDao {

    @Query("SELECT EXISTS(SELECT 1 FROM question_sets)")
    suspend fun hasAnyQuestionSets(): Boolean

    @Query("SELECT * FROM question_sets ORDER BY name ASC")
    fun getAllQuestionSetsOverview(): Flow<List<QuestionSetEntity>>

    @Transaction
    @Query("SELECT * FROM question_sets WHERE id = :id")
    fun getQuestionSetById(id: Long): Flow<QuestionSetWithDetails?>

    @Insert
    suspend fun insertQuestionSet(questionSet: QuestionSetEntity): Long

    @Insert
    suspend fun insertQuestionSetSection(section: QuestionSetSectionEntity): Long

    @Insert
    suspend fun insertQuestionSetItem(item: QuestionSetItemEntity): Long

    @Insert
    suspend fun insertQuestionSetItemOptions(options: List<QuestionSetItemOptionEntity>)

    /**
     * Generic import: builds a full question set from scratch. Mirrors
     * ChecklistDao.createChecklist's shape — surrogate ids round-trip through
     * Kotlin since onConflict isn't REPLACE here (a fresh autogenerate insert,
     * id collisions shouldn't happen and would indicate a real bug).
     */
    @Transaction
    suspend fun importQuestionSet(
        name: String,
        sectionsWithItems: Map<QuestionSetSectionEntity, List<Pair<QuestionSetItemEntity, List<String>>>>
    ): Long {
        val questionSetId = insertQuestionSet(QuestionSetEntity(name = name))

        sectionsWithItems.forEach { (section, itemsWithOptions) ->
            val sectionId = insertQuestionSetSection(section.copy(questionSetId = questionSetId))

            itemsWithOptions.forEach { (item, optionTexts) ->
                val itemId = insertQuestionSetItem(item.copy(sectionId = sectionId))

                if (optionTexts.isNotEmpty()) {
                    insertQuestionSetItemOptions(
                        optionTexts.mapIndexed { index, text ->
                            QuestionSetItemOptionEntity(itemId = itemId, position = index, text = text)
                        }
                    )
                }
            }
        }

        return questionSetId
    }

}