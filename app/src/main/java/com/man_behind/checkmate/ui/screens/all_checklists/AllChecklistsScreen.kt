package com.man_behind.checkmate.ui.screens.all_checklists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    // TODO handler error snackbar

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
        onChecklistClick = onChecklistClick
    )
}


@Composable
fun AllChecklistsContent(
    uiState: AllChecklistsUiState,
    onAddClick: () -> Unit,
    onChecklistClick: (Long) -> Unit
){
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = uiState.checklists,
                        key = { it.id }
                    ) { checklist ->
                        ChecklistOverviewItem(
                            item = checklist,
                            onClick = onChecklistClick
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
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
        onChecklistClick = {}
    )
}