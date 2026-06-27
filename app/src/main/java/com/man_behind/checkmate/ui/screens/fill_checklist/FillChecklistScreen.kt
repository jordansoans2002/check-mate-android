package com.man_behind.checkmate.ui.screens.fill_checklist

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.man_behind.checkmate.R
import com.man_behind.checkmate.data.model.Checklist
import com.man_behind.checkmate.data.model.ChecklistItem
import com.man_behind.checkmate.data.model.ChecklistItemOption
import com.man_behind.checkmate.data.model.ChecklistSection
import com.man_behind.checkmate.data.repository.ChecklistRepositoryMockImpl
import com.man_behind.checkmate.ui.components.ChecklistItemRow
import com.man_behind.checkmate.ui.components.GuidelineBottomSheet
import com.man_behind.checkmate.ui.components.GuidelineTooltip
import com.man_behind.checkmate.ui.components.ImageSourcePicker
import com.man_behind.checkmate.ui.components.ImageViewerDialog
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

@Composable
fun FillChecklistScreen(
    viewModel: FillChecklistViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val checklist by viewModel.checklist.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var activeSectionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var restoredPosition by rememberSaveable { mutableStateOf(false) }

    var showGuidelineTooltip by remember { mutableStateOf<String?>(null) }
    var showGuidelineBottomSheet by remember { mutableStateOf<String?>(null) }
    var showImageViewer by remember { mutableStateOf<Pair<List<Uri>, Int>?>(null) }
    var showImageSourcePicker by remember { mutableStateOf(false) }
    var currentItemForImages by remember { mutableStateOf<ChecklistItem?>(null) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, flag)
                } catch (e: Exception) {
                    Log.e("gallery launcher", "Failed to take permission $e")
                }
            }
            currentItemForImages?.let { item ->
                viewModel.onGalleryImagesAdded(uris, item)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedImageUri?.let { uri ->
                currentItemForImages?.let { item ->
                    viewModel.onCameraImageCaptured(uri, item)
                }
            }
        }
    }

    // Handles app background and navigation away
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.flushAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.flushAll()
        }
    }

    LaunchedEffect(checklist) {
        if (checklist != null && !restoredPosition) {
            activeSectionId = checklist!!.lastModifiedSectionId
            restoredPosition = true
        }
    }

    checklist?.let { checklist ->
        val activeSectionIndex = checklist.sections
            .indexOfFirst { it.id == activeSectionId }
            .let { if (it < 0) 0 else it }

        val lastModifiedItemIndex = checklist.sections
            .getOrNull(activeSectionIndex)
            ?.let { section ->
                section.items.indexOfFirst { it.id == section.lastModifiedItemId }
                    .let { if (it < 0) 0 else it }
            } ?: 0

        val listState = rememberLazyListState()
        LaunchedEffect(restoredPosition, activeSectionIndex) {
            if (restoredPosition) {
                snapshotFlow { listState.layoutInfo.totalItemsCount }
                    .first { it > 0 }
                listState.scrollToItem(lastModifiedItemIndex)
            }
        }

        FillChecklistContent(
            checklist = checklist,
            activeSectionIndex = activeSectionIndex,
            listState = listState,
            onSectionChange = { activeSectionId = checklist.sections[activeSectionIndex + it].id },
            getController = { viewModel.getController(it) },
            onGuidelineClick = { text ->
                showGuidelineBottomSheet = text
//                if (text.length < 100) {
//                    showGuidelineTooltip = text
//                } else {
//                    showGuidelineBottomSheet = text
//                }
            },
            onImageClick = { uris, index ->
                showImageViewer = uris to index
            },
            onAddImageClick = { item ->
                currentItemForImages = item
                showImageSourcePicker = true
            },
            onRemoveImageClick = { item, imageId, uri ->
                viewModel.onImageRemoved(item ,imageId, uri)
            }
        )
    }

    showGuidelineBottomSheet?.let { guideline ->
        GuidelineBottomSheet(
            text = guideline,
            onDismiss = { showGuidelineBottomSheet = null }
        )
    }
    // TODO anchor to icon
    showGuidelineTooltip?.let { guideline ->
        GuidelineTooltip(
            text = guideline,
            onDismiss = { showGuidelineTooltip = null }
        )
    }

    showImageViewer?.let { (uris, startIndex) ->
        ImageViewerDialog(
            uris = uris,
            startIndex = startIndex,
            onDismiss = { showImageViewer = null }
        )
    }


    if (showImageSourcePicker) {
        ImageSourcePicker(
            onDismiss = { showImageSourcePicker = false },
            onGalleryClick = {
                showImageSourcePicker = false
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onCameraClick = {
                showImageSourcePicker = false
                val uri = viewModel.mediaManager.getTempCameraUri()
                capturedImageUri = uri
                cameraLauncher.launch(uri)
            }
        )
    }
}

@Composable
fun FillChecklistContent(
    checklist: Checklist,
    activeSectionIndex: Int,
    listState: LazyListState = rememberLazyListState(),
    onSectionChange: (Int) -> Unit,
    getController: (ChecklistItem) -> ChecklistItemEditController,
    onGuidelineClick: (String) -> Unit,
    onImageClick: (List<Uri>, Int) -> Unit,
    onAddImageClick: (ChecklistItem) -> Unit,
    onRemoveImageClick: (ChecklistItem, Long, Uri) -> Unit,
) {
    val currentSection = checklist.sections[activeSectionIndex]

    var totalItems = 0
    var totalItemsChecked = 0
    var sectionItemsChecked = 0
    checklist.sections.forEachIndexed { index, section ->
        totalItems += section.items.size
        for (item in section.items) {
            if (
                item.selectedOptionId != null ||
                item.comment.isNotBlank() ||
                item.actionTaken.isNotBlank() ||
                item.images.isNotEmpty()
            ) {
                totalItemsChecked++

                if (index == activeSectionIndex)
                    sectionItemsChecked++
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton (
                modifier = Modifier.width(84.dp),
                enabled = activeSectionIndex > 0,
                onClick = { onSectionChange(-1) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ){
                    if (activeSectionIndex > 0) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = stringResource(R.string.previous_section),
                        )
                        Text(
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            text = checklist.sections[activeSectionIndex - 1].name
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(
                        R.string.progress,
                        activeSectionIndex+1,
                        checklist.sections.size
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = checklist.sections[activeSectionIndex].name,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            TextButton (
                modifier = Modifier.width(96.dp),
                enabled = activeSectionIndex < checklist.sections.size - 1,
                onClick = { onSectionChange(1) },
                shape = RoundedCornerShape(8.dp)
            ) {
                if (activeSectionIndex < checklist.sections.size -1) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.next_section),
                        )
                        Text(
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            text = checklist.sections[activeSectionIndex + 1].name
                        )
                    }
                }
            }
        }
        ChecklistProgressBar(sectionItemsChecked, currentSection.items.size)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = listState,
        ) {
            items(
                items = currentSection.items,
                key = { it.id }
            ) { item ->
                ChecklistItemRow(
                    item = item,
                    controller = getController(item),
                    onGuidelineClick = onGuidelineClick,
                    onImageClick = onImageClick,
                    onAddImageClick = { onAddImageClick(item) },
                    onRemoveImageClick = { imageId, uri -> onRemoveImageClick(item, imageId, uri) }
                )
            }
        }


        Column (
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = checklist.name
            )

            ChecklistProgressBar(totalItemsChecked, totalItems)
        }
    }
}

@Composable
fun ChecklistProgressBar(completed: Int, total: Int) {
    Box(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            modifier = Modifier.matchParentSize(),
            progress = { completed.toFloat() / total },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
        )
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = stringResource(
                R.string.progress,
                completed,
                total,
            ),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FillChecklistContentPreview() {
    val scope = rememberCoroutineScope()
    val controllers = remember { mutableMapOf<Long, ChecklistItemEditController>() }
    val mockRepository = remember { ChecklistRepositoryMockImpl() }

    FillChecklistContent(
        checklist = Checklist(
            id = 1,
            name = "Inspection Checklist",
            sections = listOf(
                ChecklistSection(
                    id = 1,
                    position = 0,
                    name = "General",
                    items = listOf(
                        ChecklistItem(
                            id = 101,
                            question = "This is a sample question. Do you have any questions about it?",
                            guidelines = "Guidelines for this question.",
                            options = listOf(
                                ChecklistItemOption(1, 0,"Yes"),
                                ChecklistItemOption(2, 1, "No"),
                                ChecklistItemOption(3, 2, "N/A")
                            ),
                            position = 1,
                            fromDocumentation = true,
                            onInspection = false,
                        )
                    )
                ),
                ChecklistSection(
                    id = 2,
                    position = 0,
                    name = "General",
                    items = listOf(
                        ChecklistItem(
                            id = 102,
                            question = "This is a sample question. Do you have any questions about it?",
                            guidelines = "Guidelines for this question.",
                            options = listOf(
                                ChecklistItemOption(1, 0,"Yes"),
                                ChecklistItemOption(2, 1,"No"),
                                ChecklistItemOption(3, 2, "N/A")
                            ),
                            position = 1,
                            fromDocumentation = true,
                            onInspection = false,
                        )
                    )
                ),
                ChecklistSection(
                    id = 3,
                    position = 0,
                    name = "General",
                    items = listOf(
                        ChecklistItem(
                            id = 103,
                            question = "This is a sample question. Do you have any questions about it?",
                            guidelines = "Guidelines for this question.",
                            options = listOf(
                                ChecklistItemOption(1, 0,"Yes"),
                                ChecklistItemOption(2, 1,"No"),
                                ChecklistItemOption(3, 2, "N/A")
                            ),
                            position = 1,
                            fromDocumentation = true,
                            onInspection = false,
                        )
                    )
                )
            ),
            createdOn = LocalDateTime.now(),
            questionSetId = 1,
            comments = "",
            lastModifiedSectionId = null,
            lastModifiedOn = null
        ),
        activeSectionIndex = 1,
        onSectionChange = { },
        onGuidelineClick = {  },
        onImageClick = { _, _ -> },
        onAddImageClick = { },
        onRemoveImageClick = { _, _, _ -> },
        getController = { item ->
            controllers.getOrPut(item.id) {
                ChecklistItemEditController(
                    itemId = item.id,
                    initialAction = item.actionTaken,
                    initialComment = item.comment,
                    repository = mockRepository,
                    scope = scope
                )
            }
        }
    )
}
