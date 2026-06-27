package com.man_behind.checkmate.ui.screens.all_checklists

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.man_behind.checkmate.R
import com.man_behind.checkmate.data.model.ChecklistOverview
import com.man_behind.checkmate.ui.components.ChecklistOverviewItem
import com.man_behind.checkmate.ui.components.CreateChecklistDialog
import kotlinx.coroutines.flow.collectLatest


@Composable
fun AllChecklistsScreen(
    viewModel: AllChecklistsViewModel = hiltViewModel(),
    onChecklistClick: (Long) -> Unit
){
    val uiState by viewModel.state.collectAsState()
    val context = LocalContext.current

    val isSelecting = uiState.selectedChecklists.isNotEmpty()

    BackHandler(enabled = isSelecting) {
        viewModel.clearSelection()
    }

    uiState.toast?.let {
        Toast.makeText(context, it, Toast.LENGTH_SHORT)
            .show()
        viewModel.clearToast()
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        Log.d("AllChecklistsScreen", "doc uri: $uri")
        if (uri == null) {
            viewModel.onExportResultConsumed()
            return@rememberLauncherForActivityResult
        }
        context.contentResolver.openOutputStream(uri)?.let { viewModel.onSaveToUri(it) }
            ?:viewModel.onExportResultConsumed()
    }

    val openTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) {
            viewModel.onExportResultConsumed()
            return@rememberLauncherForActivityResult
        }
        viewModel.onSaveToFolder(context.contentResolver, uri)
    }

    if (uiState.showSaveExportDialog) {
        if (viewModel.tempFiles.size == 1) {
            val name = uiState.checklists
                .find {
                    it.id == uiState.selectedChecklists.firstOrNull()
                }?.name
                ?.replace(Regex("[^a-zA-Z0-9 _-]"), "")
                ?.trim() ?: ""
                .ifBlank { "checklist" }
            createDocumentLauncher.launch(name+  ".pdf")
            viewModel.hideSaveExportDialog()
        } else {
            openTreeLauncher.launch(null)
            viewModel.hideSaveExportDialog()
        }
    }

    if (uiState.showCreateChecklistDialog) {
        CreateChecklistDialog(
            questionSets = uiState.questionSets,
            onDismiss = { viewModel.toggleCreateChecklistDialog(false) },
            onCreate = { id, name ->
                if (id == null) {
                    // TODO show error
                    return@CreateChecklistDialog
                }
                viewModel.createChecklist(id, name)
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.newChecklistId
            .collectLatest { id ->
                onChecklistClick(id)
            }
    }

    AllChecklistsContent(
        uiState = uiState,
        onAddClick = {
            if (uiState.questionSets.isEmpty())
                return@AllChecklistsContent

            viewModel.toggleCreateChecklistDialog(true)
        },
        onChecklistClick = { id ->
            if (isSelecting) viewModel.onToggleSelect(id)
            else onChecklistClick(id)
        },
        onChecklistLongClick = { viewModel.onLongClick(it) },
        onClearSelection = { viewModel.clearSelection() },
        onExport = { viewModel.onExportClick() },
        onDelete = { viewModel.onDeleteClick() }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllChecklistsContent(
    uiState: AllChecklistsUiState,
    onAddClick: () -> Unit,
    onChecklistClick: (Long) -> Unit,
    onChecklistLongClick: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
){
    val isSelecting = uiState.selectedChecklists.isNotEmpty()

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isSelecting,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                SelectionTopBar(
                    isLoading = uiState.isLoading,
                    selectedCount = uiState.selectedChecklists.size,
                    onExport = onExport,
                    onClear = onClearSelection,
                    onDelete = onDelete
                )
            }
        }
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.checklists.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.no_checklist_text),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = paddingValues,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = uiState.checklists,
                            key = { it.id }
                        ) { checklist ->
                            ChecklistOverviewItem(
                                item = checklist,
                                isSelected = checklist.id in uiState.selectedChecklists,
                                onClick = onChecklistClick,
                                onLongClick = onChecklistLongClick
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = !isSelecting,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                FloatingActionButton(
                    modifier = Modifier
                        .padding(16.dp),
                    onClick = onAddClick,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_checklist_description)
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .pointerInput(Unit) {},
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp),
                        strokeWidth = 4.dp
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    isLoading: Boolean,
    selectedCount: Int,
    onExport: () -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        title = {
            Text("${selectedCount} selected")
        },
        navigationIcon = {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_selection_description))
            }
        },
        actions = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onExport) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource(R.string.export_checklist_description))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_checklist_description))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor          = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor       = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor  = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}


@Preview(showBackground = true)
@Composable
fun AllChecklistsContentPreview() {
    val sampleData = listOf(
        ChecklistOverview(
            id = 1,
            questionSetId = 1,
            name = "Inspection Checklist",
            progress = listOf(0.2f),
            createdOn = "14-03-25",
        ),
        ChecklistOverview(
            id = 2,
            questionSetId = 1,
            name = "Safety Audit",
            progress = listOf(0.2f),
            createdOn = "14-03-25",
        )
    )

    AllChecklistsContent(
        uiState = AllChecklistsUiState(
            checklists = sampleData,
            isLoading = false
        ),
        onAddClick = {},
        onChecklistClick = {},
        onChecklistLongClick = {},
        onClearSelection = {  },
        onExport = {  },
        onDelete = {  }
    )
}