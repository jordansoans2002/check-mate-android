package com.man_behind.checkmate.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
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
    controller: ChecklistItemEditController,
    onGuidelineClick: (String) -> Unit,
    onAddImageClick: () -> Unit,
) {
    val action by controller.action.collectAsState()
    val comment by controller.comment.collectAsState()

    ChecklistItemRowContent(
        modifier = modifier,
        item = item,
        onGuidelineClick = onGuidelineClick,
        onOptionSelected = controller::onOptionSelected,
        action = action,
        onActionChanged = controller::onActionChanged,
        comment = comment,
        onCommentChanged = controller::onCommentChanged,
        onImageSelected = { /* Open image viewer if needed */ },
        onImageRemoved = controller::onImagesRemoved,
        onAddImageClick = onAddImageClick,
        flush = controller::flush
    )
}

@Composable
fun ChecklistItemRowContent(
    modifier: Modifier,
    item: ChecklistItem,
    onGuidelineClick: (String) -> Unit,
    onOptionSelected: (Long?) -> Unit,
    action: String,
    onActionChanged: (String) -> Unit,
    comment: String,
    onCommentChanged: (String) -> Unit,
    onImageSelected: (Uri) -> Unit,
    onImageRemoved: (Long) -> Unit,
    onAddImageClick: () -> Unit,
    flush: () -> Unit,
) {
    val annotatedText = buildAnnotatedString {
        append(item.question)

        if (item.fromDocumentation || item.onInspection) {
            append(" (")
            if (item.fromDocumentation) append("M")
            if (item.onInspection) append("V")
            append(")")
        }

        if (!item.guidelines.isNullOrBlank()) {
            append(" ")
            appendInlineContent("info_icon", "[i]")
        }
    }

    val inlineContent = if (!item.guidelines.isNullOrBlank()) {
        mapOf(
            "info_icon" to InlineTextContent(
                Placeholder(
                    width = 16.sp,
                    height = 16.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Top
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.guidelines_label),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(16.dp)
                        .offset(y = (-0).dp)
                        .clip(CircleShape)
                        .clickable { onGuidelineClick(item.guidelines) }
                )
            }
        )
    } else
        mapOf()

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
            Text(
                text = annotatedText,
                inlineContent = inlineContent,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
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

                Box(modifier = Modifier.weight(1f))

                FilterChip(
                    selected = item.selectedOptionId == null,
                    onClick = { onOptionSelected(null) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear_option_selected_description)
                        )
                    },
                    label = { Text(stringResource(R.string.clear_option_selected_label)) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) flush() },
                value = comment,
                onValueChange = onCommentChanged,
                label = { Text("Comment") },
                singleLine = false,
                minLines = 1,
                maxLines = 5,
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) flush() },
                value = action,
                onValueChange = onActionChanged,
                label = { Text("Action Taken") },
                singleLine = false,
                minLines = 1,
                maxLines = 3,
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (item.images.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(items = item.images, key = { it.id }) { image ->
                        ImageThumbnail(
                            imageUri = image.uri,
                            onClick = { onImageSelected(image.uri) },
                            onRemove = { onImageRemoved(image.id) }
                        )
                    }
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                onClick = onAddImageClick,
            ) {
                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = stringResource(R.string.add_image_label)
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(stringResource(R.string.add_image_label))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChecklistItemRowPreview() {
    val item = ChecklistItem(
        id = 1,
        question = "This is a sample question. Do you have any questions about it?",
        guidelines = "Guidelines",
        options = listOf(
            ChecklistItemOption(1,  0,"Option 1"),
            ChecklistItemOption(2,  1,"Option 2"),
        ),
        selectedOptionId = 2,
        fromDocumentation = true,
        onInspection = false,
        images = emptyList(),
        position = 1
    )
    ChecklistItemRowContent(
        modifier = Modifier.padding(),
        item = item,
        onGuidelineClick =  {  },
        onOptionSelected = {  },
        action = "",
        onActionChanged = {  },
        comment = "",
        onCommentChanged = {  },
        onImageSelected = {  },
        onImageRemoved = {  },
        onAddImageClick = {  },
    ) { }
}
