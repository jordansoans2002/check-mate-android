package com.man_behind.checkmate.ui.screens.fill_checklist

import android.net.Uri
import android.util.Log
import com.man_behind.checkmate.data.repository.ChecklistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

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
            commentFlow
                .debounce(700.milliseconds)
                .distinctUntilChanged()
                .collectLatest { new ->
                    if (new != lastSavedComment) {
                        repository.updateComment(itemId, new)
                    }
                }
        }

        scope.launch {
            actionFlow
                .debounce(700.milliseconds)
                .distinctUntilChanged()
                .collectLatest { new ->
                    if (new != lastSavedAction) {
                        repository.updateActionTaken(itemId, new)
                    }
                }
        }
    }

    fun onCommentChanged(text: String) {
        commentFlow.value = text
    }

    fun onActionChanged(text: String) {
        actionFlow.value = text
    }

    fun onOptionSelected(optionId: Long?) {
        scope.launch {
            repository.updateSelectedOption(
                itemId = itemId,
                optionId =  optionId
            )
        }
    }

    fun onImagesAdded(images: List<Uri>) {
        scope.launch {
            repository.addImages(
                itemId = itemId,
                images = images
            )
        }
    }

    fun onImageRemoved(itemImageId: Long) {
        scope.launch {
            repository.removeImage(itemId, itemImageId)
        }
    }

    fun flush() {
        val action = actionFlow.value
        val comment = commentFlow.value

        val needsAction = action != lastSavedAction
        val needsComment = comment != lastSavedComment

        scope.launch {
            if(needsComment && needsAction){
                repository.saveCommentAction(
                    itemId = itemId,
                    comment = comment,
                    actionTaken = action
                )
                lastSavedComment = comment
                lastSavedAction = action
            } else if (needsComment) {
                repository.updateComment(itemId, comment)
                lastSavedComment = comment
            } else if (needsAction) {
                repository.updateActionTaken(itemId, action)
                lastSavedAction = action
            } else
                return@launch
        }
    }
}