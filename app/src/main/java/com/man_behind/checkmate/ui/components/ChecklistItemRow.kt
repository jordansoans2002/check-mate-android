package com.man_behind.checkmate.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.man_behind.checkmate.R
import com.man_behind.checkmate.data.model.ChecklistItem
import com.man_behind.checkmate.data.model.ChecklistItemOption
import com.man_behind.checkmate.ui.screens.fill_checklist.ChecklistItemEditController

@Composable
fun ChecklistItemRow(
    modifier: Modifier = Modifier,
    item: ChecklistItem,
    controller: ChecklistItemEditController
) {

    val action by controller.action.collectAsState()
    val comment by controller.comment.collectAsState()

    ChecklistItemRowContent(
        modifier,
        item,
        controller::onOptionSelected,
        action,
        controller::onActionChanged,
        comment,
        controller::onCommentChanged,
        {  }, { _, _ ->  },
        {  },
        controller::flush
    )
}


@Composable
fun ChecklistItemRowContent(
    modifier: Modifier,
    item: ChecklistItem,
    onOptionSelected: (Long?) -> Unit,
    action: String,
    onActionChanged: (String) -> Unit,
    comment: String,
    onCommentChanged: (String) -> Unit,
    onImageSelected: (Uri) -> Unit,
    onImageRemoved: (Uri, Int) -> Unit,
    onAddImageClick: () -> Unit,
    flush: () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = buildAnnotatedString {
                append(item.question)

                if (item.fromDocumentation || item.onInspection) {
                    withStyle(
                        style = SpanStyle(
                            fontSize = 11.sp, // smaller text
                            baselineShift = BaselineShift.Superscript
                        )
                    ) {
                        append(" (")
                        if (item.fromDocumentation)
                            append("M")
                        if (item.onInspection)
                            append("V")
                        append(")")
                    }
                }
            },
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item.options.forEach { option ->
                FilterChip(
                    selected = option.id == item.selectedOptionId,
                    onClick = { onOptionSelected(option.id) },
                    label = { Text(option.text) },
                    leadingIcon = if (option.id == item.selectedOptionId) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }

            // Clear chip
            FilterChip(
                selected = item.selectedOptionId == null,
                onClick = { onOptionSelected(null) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = stringResource(R.string.clear_option_selected_description)
                    )
                },
                label = {
                    Text(
                        stringResource(R.string.clear_option_selected_label)
                    )
                }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (!it.isFocused) flush()
                },
            value = comment,
            onValueChange = { onCommentChanged(it) },
            placeholder = { Text("Comment") },
            singleLine = false,
            minLines = 3,
            maxLines = 5,
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (!it.isFocused) flush()
                },
            value = action,
            onValueChange = { onActionChanged(it) },
            placeholder = { Text("Action Taken") },
            singleLine = false,
            minLines = 3,
            maxLines = 3,
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow {
            items(items = item.images, key = { it.toString() }) { imageUri ->
                ImageThumbnail(
                    imageUri = imageUri,
                    onClick = { onImageSelected(imageUri) },
                    onRemove = { onImageRemoved(imageUri, 0) }
                )
            }
        }

        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = onAddImageClick,
        ) {
            Icon(
                imageVector = Icons.Default.AddAPhoto,
                contentDescription = stringResource(R.string.add_image_label)
            )
            Text(stringResource(R.string.add_image_label))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChecklistItemRowPreview() {
    val item = ChecklistItem(
        id = 1,
        question = "Question",
        guidelines = "Guidelines",
        options = listOf(
            ChecklistItemOption(1, "Option 1"),
            ChecklistItemOption(2, "Option 2"),
        ),
        selectedOptionId = 2,
        fromDocumentation = true,
        onInspection = false,
        images = emptyList(),
    )
    ChecklistItemRowContent(
        modifier = Modifier.padding(4.dp),
        item = item,
        onOptionSelected = {  },
        action = "",
        onActionChanged = {  },
        comment = "",
        onCommentChanged = {  },
        onImageSelected = {  },
        onImageRemoved = { _, _ -> },
        onAddImageClick = {  },
        flush = {  }
    )
}