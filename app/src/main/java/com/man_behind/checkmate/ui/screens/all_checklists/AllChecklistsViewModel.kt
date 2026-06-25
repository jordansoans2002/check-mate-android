package com.man_behind.checkmate.ui.screens.all_checklists

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.man_behind.checkmate.data.model.Checklist
import com.man_behind.checkmate.data.repository.ChecklistRepository
import com.man_behind.checkmate.data.repository.QuestionSetRepository
import com.man_behind.checkmate.utils.ChecklistPdfExporter
import com.man_behind.checkmate.utils.ChecklistPdfExporter2
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class AllChecklistsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val checklistRepository: ChecklistRepository,
    private val questionSetRepository: QuestionSetRepository,
): ViewModel() {

    private val _state = MutableStateFlow(AllChecklistsUiState())
    val state: StateFlow<AllChecklistsUiState> =
        combine(
            _state,
            checklistRepository.getAllChecklists(),
            questionSetRepository.getAllQuestionSets(),
        ) { state, checklists, questionSets ->
            state.copy(
                checklists = checklists,
                questionSets = questionSets,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            initialValue = AllChecklistsUiState(isLoading = true)
        )

    private var pdfExporter: ChecklistPdfExporter? = null

    override fun onCleared() {
        pdfExporter?.release()
    }

    fun toggleCreateChecklistDialog(show: Boolean) {
        _state.update { it.copy(showCreateChecklistDialog = show) }
    }

    fun createChecklist(questionSetId: Long, name: String) {
        viewModelScope.launch {
            val checklistId = checklistRepository.createChecklist(questionSetId,name)
            _state.update { it.copy(
                newChecklistId = checklistId,
                showCreateChecklistDialog = false,
            ) }
        }
    }

    fun onLongClick(id: Long) { onToggleSelect(id) }

    fun onToggleSelect(id: Long) {
        _state.update { state ->
            val newIds = if (id in state.selectedChecklists)
                state.selectedChecklists - id
            else
                state.selectedChecklists + id

            state.copy(selectedChecklists = newIds)
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedChecklists = emptySet()) }
    }

    fun onExportClick() {
        val ids = _state.value.selectedChecklists.toList()
        if (ids.isEmpty()) return

        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            if (ids.size == 1) exportSingle(ids.first())
            else exportMultiple(ids)
        }
    }

    // Single export: render one PDF then ask user where to save it.
    private suspend fun exportSingle(id: Long) {
        val checklist = checklistRepository.getChecklistById(id)
            .first()
        if (checklist == null) {
            _state.update { it.copy(toast = "Checklist not found") }
            return
        }

        val tempFile = File(context.cacheDir, "export_${id}.pdf")

        // ChecklistPdfExporter.export() calls back on the MAIN thread (WebView requirement).
        // We drive it from a coroutine using suspendCoroutine so the rest of the VM
        // stays coroutine-friendly.
        val success = renderPdf(checklist, tempFile)

        if (success) {
            _state.update { it.copy(tempFiles = listOf(tempFile)) }
        } else {
            _state.update { it.copy(toast = "Failed to generate PDF") }
        }
    }

    // Multi export: render all PDFs into cache, then ask user for a folder.
    private suspend fun exportMultiple(ids: List<Long>) {
        val tempFiles = mutableListOf<File>()

        for (id in ids) {
            val checklist = checklistRepository.getChecklistById(id).first() ?: continue
            val tempFile  = File(context.cacheDir, "export_${id}.pdf")

            val success = renderPdf(checklist, tempFile)
            if (success) tempFiles.add(tempFile)
        }

        if (tempFiles.isEmpty()) {
            _state.update { it.copy(toast = "Failed to generate PDFs") }
            return
        }

        _state.update { it.copy(tempFiles = tempFiles) }
    }

    /**
     * Wraps [ChecklistPdfExporter.export] in a suspending call.
     * Must be called from a coroutine; internally it resumes on the main thread
     * because WebView requires it.
     */
    private suspend fun renderPdf(
        checklist: Checklist,
        outputFile: File,
    ): Boolean = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        pdfExporter?.release()
        pdfExporter = ChecklistPdfExporter(context)

        // Export calls onComplete on the main thread already
        pdfExporter!!.export(checklist, outputFile) { success ->
            if (cont.isActive) cont.resume(success) { cause, _, _ -> }
        }
    }

    // ── SAF callbacks (called from the screen after the picker returns) ───────

    /** Single-file save: write the temp PDF into the URI the SAF picker gave us. */
    fun onSaveToUri(outputStream: OutputStream) {
        val tempFiles = _state.value.tempFiles
        if (tempFiles?.size != 1) return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                tempFiles[0].inputStream().use { it.copyTo(outputStream) }
                outputStream.close()
                tempFiles[0].delete()
            }.onSuccess {
                _state.update { it.copy(isLoading = false, selectedChecklists = emptySet()) }
            }.onFailure { e ->
                _state.update { it.copy(toast = "Could not save: ${e.message}") }
            }
        }
    }

    /**
     * Multi-file save: write every temp PDF into [folderUri].
     * Each file is created via ContentResolver so no MANAGE_EXTERNAL_STORAGE is needed.
     */
    fun onSaveToFolder(contentResolver: android.content.ContentResolver, folderUri: android.net.Uri) {
        val tempFiles = _state.value.tempFiles ?: return

        viewModelScope.launch(Dispatchers.IO) {
            var saved = 0
            for (tempFile in tempFiles) {
                runCatching {
                    // Build a child document URI inside the chosen tree
                    val docUri = androidx.documentfile.provider.DocumentFile
                        .fromTreeUri(context, folderUri)
                        ?.createFile("application/pdf", tempFile.nameWithoutExtension)
                        ?.uri ?: return@runCatching

                    contentResolver.openOutputStream(docUri)?.use { out ->
                        tempFile.inputStream().copyTo(out)
                    }
                    tempFile.delete()
                    saved++
                }
            }

            if (saved > 0) {
                _state.update {
                    it.copy(
                        toast = "$saved PDF${if (saved > 1) "s" else ""} saved",
                        selectedChecklists = emptySet(),
                    )
                }
            } else {
                _state.update { it.copy(toast = "Failed to save files") }
            }
        }
    }

    fun onExportResultConsumed() {
        _state.update { it.copy(isLoading = false) }
    }

    private fun String.sanitizeFileName() =
        replace(Regex("[^a-zA-Z0-9 _-]"), "").trim().ifBlank { "checklist" }

    fun clearError() {
        _state.update { it.copy(toast = null) }
    }

    fun clearNewChecklist() {
        _state.update { it.copy(newChecklistId = null) }
    }


}