package com.man_behind.checkmate.data.local.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.MapColumn
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.man_behind.checkmate.data.local.db.entity.ChecklistEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistItemEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistItemImageEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistItemOptionEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistSectionEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistWithDetails
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ChecklistDao {

    @Query("""
        SELECT 
            lists.*,
            (CAST(COUNT(items.selectedOptionId) AS FLOAT) / COUNT(items.id)) as sectionProgress
        FROM checklists lists
        JOIN checklist_sections sections ON sections.checklistId = lists.id
        LEFT JOIN checklist_items items ON items.sectionId = sections.id
        GROUP BY sections.id
        ORDER BY lists.lastModifiedOn DESC
    """)
    fun getAllChecklistsOverview(): Flow<Map<ChecklistEntity, List<@MapColumn(columnName = "sectionProgress") Float>>>

    @Query("""
       SELECT * FROM checklists lists
       WHERE lists.id = :id
    """)
    fun getChecklistById(id: Long): Flow<ChecklistWithDetails?>

    @Insert
    suspend fun insertChecklist(checklist: ChecklistEntity): Long

    @Insert
    suspend fun insertSections(sections: List<ChecklistSectionEntity>): List<Long>

    @Insert
    suspend fun insertItems(items: List<ChecklistItemEntity>): List<Long>

    @Insert
    suspend fun insertOptions(options: List<ChecklistItemOptionEntity>)

    @Transaction
    suspend fun createChecklist(
        checklist: ChecklistEntity,
        sectionsWithItems: Map<ChecklistSectionEntity, List<Pair<ChecklistItemEntity, List<String>>>>
    ) {
        val checklistId = insertChecklist(checklist)

        sectionsWithItems.forEach { (section, itemWithTags) ->
            val sectionId = insertSections(listOf(section.copy(checklistId = checklistId))).first()

            itemWithTags.forEach { (item, options) ->
                val itemId = insertItems(listOf(item.copy(
                    checklistId = checklistId,
                    sectionId = sectionId
                ))).first()

                val optionEntities = options.map {
                    ChecklistItemOptionEntity(checklistItemId = itemId, text = it)
                }
                insertOptions(optionEntities)
            }
        }
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
            SELECT checklistId FROM checklist_items
            WHERE id = :itemId
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
}