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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.man_behind.checkmate.R
import com.man_behind.checkmate.data.model.ChecklistOverview
import com.man_behind.checkmate.ui.components.ChecklistOverviewItem
import com.man_behind.checkmate.ui.components.CreateChecklistDialog


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
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
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

    LaunchedEffect(uiState.tempFiles) {
        uiState.tempFiles?.let { tempFiles ->
            if (tempFiles.size == 1) {
                val name = uiState.checklists
                    .find {
                        it.id == uiState.selectedChecklists.firstOrNull()
                    }?.name
                    ?: "Checklist"
                createDocumentLauncher.launch(name+  ".pdf")
            } else {
                openTreeLauncher.launch(null)
            }
        }
    }

    uiState.newChecklistId?.let { checklistId ->
        onChecklistClick(checklistId)
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
        onDelete = {  }
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

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                // TODO show loading if required, currently fetching from local db
            }

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
                val topPad = if (isSelecting) 86.dp else 0.dp
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = topPad + 8.dp,
                        bottom = 8.dp
                    ),
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
            visible = isSelecting,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = {
                    Text("${uiState.selectedChecklists.size} selected")
                },
                navigationIcon = {
                    IconButton(onClick = onClearSelection) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_selection_description))
                    }
                },
                actions = {
                    if (uiState.isLoading) {
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

        AnimatedVisibility(
            visible = isSelecting,
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
    }
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