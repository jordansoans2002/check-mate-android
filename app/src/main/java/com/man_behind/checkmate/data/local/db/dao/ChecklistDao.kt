package com.man_behind.checkmate.data.local.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.MapColumn
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.man_behind.checkmate.data.local.db.entity.ChecklistEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistItemEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistItemImageEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistItemOptionEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistSectionEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistWithDetails
import com.man_behind.checkmate.data.local.db.entity.QuestionSetItemEntity
import com.man_behind.checkmate.data.local.db.entity.QuestionSetItemOptionEntity
import com.man_behind.checkmate.data.local.db.entity.QuestionSetSectionEntity
import com.man_behind.checkmate.data.mapper.toChecklistItemEntity
import com.man_behind.checkmate.data.mapper.toChecklistItemOptionEntity
import com.man_behind.checkmate.data.mapper.toChecklistSectionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ChecklistDao {

    @Query("""
        SELECT 
            lists.*,
            CASE 
                WHEN COUNT(items.id) = 0 THEN 0
                ELSE CAST(
                    SUM(
                        CASE
                            WHEN items.selectedOptionId IS NOT NULL
                                 OR TRIM(COALESCE(items.comments, '')) <> ''
                                 OR TRIM(COALESCE(items.actionTaken, '')) <> ''
                            THEN 1
                            ELSE 0
                        END
                    ) AS FLOAT
                ) / COUNT(items.id)
            END as sectionProgress
        FROM checklists lists
        JOIN checklist_sections sections ON sections.checklistId = lists.id
        LEFT JOIN checklist_items items ON items.sectionId = sections.id
        GROUP BY sections.id
        ORDER BY lists.lastModifiedOn DESC
    """)
    fun getAllChecklistsOverview(): Flow<Map<ChecklistEntity, List<@MapColumn(columnName = "sectionProgress") Float>>>

    @Transaction
    @Query("""
       SELECT * FROM checklists lists
       WHERE lists.id = :id
    """)
    fun getChecklistById(id: Long): Flow<ChecklistWithDetails?>


    @Insert
    suspend fun insertChecklist(checklistEntity: ChecklistEntity): Long

    @Query("""
        SELECT * FROM question_sections
        WHERE questionSetId = :questionSetId
    """)
    suspend fun getQuestionSetSections(questionSetId: Long): List<QuestionSetSectionEntity>

    @Insert
    suspend fun insertChecklistSection(checklistSectionEntity: ChecklistSectionEntity): Long


    @Query("""
        SELECT * FROM question_items
        WHERE sectionId = :sectionId
    """)
    suspend fun getQuestionSetItems(sectionId: Long): List<QuestionSetItemEntity>

    @Insert
    suspend fun insertChecklistItem(checklistItemEntity: ChecklistItemEntity): Long

    @Query("""
        SELECT * FROM question_item_options
        WHERE itemId = :itemId
    """)
    suspend fun getQuestionSetOptions(itemId: Long): List<QuestionSetItemOptionEntity>

    @Insert
    suspend fun insertChecklistItemOptions(checklistItemOptions: List<ChecklistItemOptionEntity>)

    @Transaction
    suspend fun createChecklist(questionSetId: Long, name: String): Long {

        val checklistId = insertChecklist(
            ChecklistEntity(
                questionSetId = questionSetId,
                name = name,
                comments = "",
                createdOn = LocalDateTime.now(),
                lastModifiedSectionId = null,
                lastModifiedOn = null
            )
        )

        val questionSetSections = getQuestionSetSections(questionSetId)
        questionSetSections.forEach { questionSetSection ->
            val sectionId = insertChecklistSection(questionSetSection.toChecklistSectionEntity(checklistId))
            getQuestionSetItems(questionSetSection.id).forEach { questionSetItem ->
                val itemId = insertChecklistItem(questionSetItem.toChecklistItemEntity(sectionId))
                val options = getQuestionSetOptions(questionSetItem.id).map { it.toChecklistItemOptionEntity(itemId) }
                insertChecklistItemOptions(options)
            }
        }

        return checklistId
    }

    @Query("""
        UPDATE checklists 
        SET 
            lastModifiedOn = :timestamp,
            lastModifiedSectionId = (
                SELECT sectionId FROM checklist_items
                WHERE id = :itemId
            )
        WHERE id = (
            SELECT s.checklistId 
            FROM checklist_items i
            JOIN checklist_sections s ON s.id = i.sectionId
            WHERE i.id = :itemId
        )
    """)
    suspend fun updateChecklistMetadata(itemId: Long, timestamp: LocalDateTime)

    @Query("""
        UPDATE checklist_sections
        SET lastModifiedItemId = :itemId
        WHERE  id = (
            SELECT sectionId FROM checklist_items
            WHERE id = :itemId
        )
    """)
    suspend fun updateSectionMetadata(itemId: Long)


    @Query("""
        UPDATE checklist_items
        SET selectedOptionId = :optionId
        WHERE id = :itemId
    """)
    suspend fun updateSelectedOption(itemId: Long, optionId: Long?)

    @Query("""
        DELETE FROM checklist_item_images
        WHERE id = :itemImageId
    """)
    suspend fun deleteImages(itemImageId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<ChecklistItemImageEntity>)

    @Query("""
        UPDATE checklist_items
        SET comment = :comment
        WHERE id = :itemId
    """)
    suspend fun updateComment(itemId: Long, comment: String)

    @Query("""
        UPDATE checklist_items
        SET actionTaken = :actionTaken
        WHERE id = :itemId
    """)
    suspend fun updateActionTaken(itemId: Long, actionTaken: String)

    @Query("""
        UPDATE checklist_items
        SET selectedOptionId = :selectedOptionId,
            actionTaken = :actionTaken,
            comment = :comment
        WHERE id = :itemId
    """)
    suspend fun updateItemDetails(
        itemId: Long,
        selectedOptionId: Long?,
        actionTaken: String,
        comment: String
    )

    // ---------------------------------------------------------------------
    // Combined field-update + metadata-update transactions.
    // Each wraps an existing single-purpose update with the two metadata
    // touches (checklist.lastModifiedOn/lastModifiedSectionId and
    // section.lastModifiedItemId) so callers never forget to stamp "last
    // modified" when editing an item. The controller decides which one to
    // call; all of them keep metadata consistent.
    // ---------------------------------------------------------------------

    @Transaction
    suspend fun updateSelectedOptionWithMetadata(
        itemId: Long,
        optionId: Long?,
        timestamp: LocalDateTime = LocalDateTime.now()
    ) {
        updateSelectedOption(itemId, optionId)
        updateChecklistMetadata(itemId, timestamp)
        updateSectionMetadata(itemId)
    }

    @Transaction
    suspend fun updateCommentWithMetadata(
        itemId: Long,
        comment: String,
        timestamp: LocalDateTime = LocalDateTime.now()
    ) {
        updateComment(itemId, comment)
        updateChecklistMetadata(itemId, timestamp)
        updateSectionMetadata(itemId)
    }

    @Transaction
    suspend fun updateActionTakenWithMetadata(
        itemId: Long,
        actionTaken: String,
        timestamp: LocalDateTime = LocalDateTime.now()
    ) {
        updateActionTaken(itemId, actionTaken)
        updateChecklistMetadata(itemId, timestamp)
        updateSectionMetadata(itemId)
    }

    @Transaction
    suspend fun updateItemDetailsWithMetadata(
        itemId: Long,
        selectedOptionId: Long?,
        actionTaken: String,
        comment: String,
        timestamp: LocalDateTime = LocalDateTime.now()
    ) {
        updateItemDetails(itemId, selectedOptionId, actionTaken, comment)
        updateChecklistMetadata(itemId, timestamp)
        updateSectionMetadata(itemId)
    }

    @Transaction
    suspend fun insertImagesWithMetadata(
        itemId: Long,
        images: List<ChecklistItemImageEntity>,
        timestamp: LocalDateTime = LocalDateTime.now()
    ) {
        insertImages(images)
        updateChecklistMetadata(itemId, timestamp)
        updateSectionMetadata(itemId)
    }

    @Transaction
    suspend fun deleteImageWithMetadata(
        itemId: Long,
        itemImageId: Long,
        timestamp: LocalDateTime = LocalDateTime.now()
    ) {
        deleteImages(itemImageId)
        updateChecklistMetadata(itemId, timestamp)
        updateSectionMetadata(itemId)
    }
}