package com.man_behind.checkmate.data.mapper

import androidx.core.net.toUri
import com.man_behind.checkmate.data.local.db.entity.ChecklistEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistItemImageEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistItemOptionEntity
import com.man_behind.checkmate.data.local.db.entity.ChecklistItemWithDetails
import com.man_behind.checkmate.data.local.db.entity.ChecklistSectionWithItems
import com.man_behind.checkmate.data.local.db.entity.ChecklistWithDetails
import com.man_behind.checkmate.data.model.Checklist
import com.man_behind.checkmate.data.model.ChecklistItem
import com.man_behind.checkmate.data.model.ChecklistItemImages
import com.man_behind.checkmate.data.model.ChecklistItemOption
import com.man_behind.checkmate.data.model.ChecklistOverview
import com.man_behind.checkmate.data.model.ChecklistSection
import java.time.format.DateTimeFormatter

fun ChecklistItemOptionEntity.toModel(): ChecklistItemOption =
    ChecklistItemOption(id = id, text = text)

fun ChecklistItemImageEntity.toModel(): ChecklistItemImages =
    ChecklistItemImages(id = id, uri = uri.toUri())

fun ChecklistItemWithDetails.toModel(): ChecklistItem =
    ChecklistItem(
        id = item.id,
        question = item.question,
        guidelines = item.guidelines,
        options = options.map { it.toModel() },
        selectedOptionId = item.selectedOptionId,
        actionTaken = item.actionTaken,
        comment = item.comment,
        fromDocumentation = item.fromDocumentation,
        onInspection = item.onInspection,
        position = item.position,
        images = images.map { it.toModel() }
    )

fun ChecklistSectionWithItems.toModel(): ChecklistSection =
    ChecklistSection(
        id = section.id,
        name = section.name,
        comments = section.comments,
        position = section.position,
        lastModifiedItemId = section.lastModifiedItemId,
        items = items.map { it.toModel() }
    )

fun ChecklistWithDetails.toModel(): Checklist =
    Checklist(
        id = checklist.id,
        questionSetId = checklist.questionSetId,
        name = checklist.name,
        comments = checklist.comments,
        createdOn = checklist.createdOn,
        lastModifiedSectionId = checklist.lastModifiedSectionId,
        lastModifiedOn = checklist.lastModifiedOn,
        sections = sections.map { it.toModel() }
    )

private val overviewDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy")

fun ChecklistEntity.toOverview(progress: List<Float>): ChecklistOverview =
    ChecklistOverview(
        id = id,
        questionSetId = questionSetId,
        name = name,
        progress = progress,
        createdOn = createdOn.format(overviewDateFormatter),
        lastModifiedOn = lastModifiedOn?.format(overviewDateFormatter)
    )