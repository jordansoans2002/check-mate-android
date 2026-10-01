package com.man_behind.checkmate.ui.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.man_behind.checkmate.ui.theme.CheckMateTheme

@Composable
fun ChecklistItemRow(
    modifier: Modifier = Modifier,
    sectionPosition: Int,
    item: ChecklistItem,
    controller: ChecklistItemEditController,
    onGuidelineClick: (String) -> Unit,
    onImageClick: (List<Uri>, Int) -> Unit,
    onAddImageClick: () -> Unit,
    onRemoveImageClick: (Long, Uri) -> Unit,
) {
    val action by controller.action.collectAsState()
    val comment by controller.comment.collectAsState()

    ChecklistItemRowContent(
        modifier = modifier,
        sectionPosition = sectionPosition,
        item = item,
        onGuidelineClick = onGuidelineClick,
        onOptionSelected = controller::onOptionSelected,
        action = action,
        onActionChanged = controller::onActionChanged,
        comment = comment,
        onCommentChanged = controller::onCommentChanged,
        onImageClick = onImageClick,
        onImageRemoved = onRemoveImageClick,
        onAddImageClick = onAddImageClick,
        flush = controller::flush
    )
}

@Composable
fun ChecklistItemRowContent(
    modifier: Modifier,
    sectionPosition: Int,
    item: ChecklistItem,
    onGuidelineClick: (String) -> Unit,
    onOptionSelected: (Long?) -> Unit,
    action: String,
    onActionChanged: (String) -> Unit,
    comment: String,
    onCommentChanged: (String) -> Unit,
    onImageClick: (List<Uri>, Int) -> Unit,
    onImageRemoved: (Long, Uri) -> Unit,
    onAddImageClick: () -> Unit,
    flush: () -> Unit,
) {
    val annotatedText = buildAnnotatedString {
        append(stringResource(R.string.question_number_question, item.questionNumber, item.question))
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

    val isAnswered = item.selectedOptionId != null ||
            item.comment.isNotBlank() ||
            item.actionTaken.isNotBlank() ||
            item.images.isNotEmpty()
    val selectedOption = item.options.find { item.selectedOptionId == it.id }?.text
    val selectedColor = when {
        !isAnswered -> Color.Black
        selectedOption.isNullOrBlank() -> Color.Transparent
        selectedOption.contains("yes", ignoreCase = true) -> Color(red = 0.0824f, green = 0.5020f, blue = 0.2392f)  // green
        selectedOption.contains("no", ignoreCase = true) -> Color(red = 0.8000f, green = 0.1020f, blue = 0.1020f)  // red
        selectedOption.contains("n/a", ignoreCase = true) -> Color(red = 0.1020f, green = 0.3961f, blue = 0.8000f)  // blue
        selectedOption.contains("n/v", ignoreCase = true) -> Color(red = 0.8000f, green = 0.5490f, blue = 0.1020f)  // yellow
        else -> Color.Transparent
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {

        Column(
            modifier = Modifier
                .drawBehind {
                    drawRect(
                        color = selectedColor,
                        topLeft = Offset.Zero,
                        size = Size(
                            width = 8.dp.toPx(),
                            height = size.height
                        )
                    )
                }
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp)
        ) {

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

                Spacer(modifier = Modifier.weight(1f))

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
                label = { Text(stringResource(R.string.comment)) },
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
                label = { Text(stringResource(R.string.action_taken)) },
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
                            onClick = {
                                onImageClick(
                                    item.images.map { it.uri },
                                    item.images.indexOf(image)
                                )
                            },
                            onRemove = { onImageRemoved(image.id, image.uri) }
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
            ChecklistItemOption(1,  0, "Yes"),
            ChecklistItemOption(2,  1, "No"),
            ChecklistItemOption(3,  2, "N/A"),
            ChecklistItemOption(3,  2, "N/A"),
            ChecklistItemOption(4,  3, "N/V"),
        ),
        selectedOptionId = null,
        fromDocumentation = true,
        onInspection = false,
        images = emptyList(),
        position = 1,
        questionNumber = "1.1"
    )

    CheckMateTheme {
        ChecklistItemRowContent(
            modifier = Modifier.padding(),
            sectionPosition = 1,
            item = item,
            onGuidelineClick =  {  },
            onOptionSelected = {  },
            action = "",
            onActionChanged = {  },
            comment = "",
            onCommentChanged = {  },
            onImageClick = { _, _ -> },
            onImageRemoved = { _, _ -> },
            onAddImageClick = {  },
            { },
        )
    }
}
