package com.man_behind.checkmate.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object AllChecklists

@Serializable
data class FillChecklist(val checklistId: Long)