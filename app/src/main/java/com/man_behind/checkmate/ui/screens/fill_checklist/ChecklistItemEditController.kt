package com.man_behind.checkmate.ui.screens.fill_checklist

import android.net.Uri
import com.man_behind.checkmate.data.repository.ChecklistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.debounce

class ChecklistItemEditController(
    private val itemId: Long,
    initialAction: String,
    initialComment: String,
    private val repository: ChecklistRepository,
    private val scope: CoroutineScope
) {

    private val actionFlow = MutableStateFlow(initialAction)
    private val commentFlow = MutableStateFlow(initialComment)

    private var lastSavedAction = initialAction
    private var lastSavedComment = initialComment

    val action = actionFlow.asStateFlow()
    val comment = commentFlow.asStateFlow()

    init {
        setupDebounce()
    }

    @OptIn(FlowPreview::class)
    private fun setupDebounce() {
        scope.launch {
            actionFlow
                .debounce(700)
                .distinctUntilChanged()
                .collectLatest { new ->
                    if (new != lastSavedAction) {
                        save(actionTaken = new)
                    }
                }
        }

        scope.launch {
            commentFlow
                .debounce(700)
                .distinctUntilChanged()
                .collectLatest { new ->
                    if (new != lastSavedComment) {
                        save(comment = new)
                    }
                }
        }
    }

    fun onActionChanged(text: String) {
        actionFlow.value = text
    }

    fun onCommentChanged(text: String) {
        commentFlow.value = text
    }

    fun onOptionSelected(optionId: Long?) {
        scope.launch {
            repository.updateSelectedOption(
                itemId = itemId,
                optionId =  optionId
            )
        }
    }

    fun onImagesUpdated(images: List<Uri>) {
        scope.launch {
            repository.updateImages(
                itemId = itemId,
                images = images
            )
        }
    }

    fun flush() {
        val action = actionFlow.value
        val comment = commentFlow.value

        val needsAction = action != lastSavedAction
        val needsComment = comment != lastSavedComment

        if (!needsAction && !needsComment)
            return

        scope.launch {
            repository.updateChecklistItem(
                itemId = itemId,
                actionTaken = if (needsAction) action else null,
                comment = if (needsComment) comment else null
            )

            if (needsAction) lastSavedAction = action
            if (needsComment) lastSavedComment = comment
        }
    }

    private fun save(
        actionTaken: String? = null,
        comment: String? = null
    ) {
        scope.launch {
            repository.updateChecklistItem(
                itemId = itemId,
                actionTaken = actionTaken,
                comment = comment
            )

            actionTaken?.let { lastSavedAction = it }
            comment?.let { lastSavedComment = it }
        }
    }
}