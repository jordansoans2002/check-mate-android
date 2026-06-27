package com.man_behind.checkmate.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.man_behind.checkmate.R
import com.man_behind.checkmate.data.model.ChecklistOverview

@Composable
fun ChecklistOverviewItem(
    modifier: Modifier = Modifier,
    item: ChecklistOverview,
    isSelected: Boolean = false,
    onClick: (Long) -> Unit,
    onLongClick: (Long) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick(item.id) },
                onLongClick = { onLongClick(item.id) }
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {

        Column(
            modifier = Modifier.matchParentSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            item.progress
                .asReversed()
                .forEachIndexed { index, sectionProgress ->

                    val delay = index * 50

                    val animatedProgress by animateFloatAsState(
                        targetValue = sectionProgress,
                        animationSpec = tween(
                            durationMillis = 500,
                            delayMillis = delay
                        ),
                        label = "progress"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        )
                    }
            }
        }

        Column(modifier = Modifier
            .padding(8.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.checklist_created_at_label,
                    item.createdOn
                ),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.checklist_updated_at_label,
                    item.lastModifiedOn ?: "-"
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier.matchParentSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
            )
        }
    }
}

@Preview
@Composable
fun ChecklistOverviewItemPreview() {
    ChecklistOverviewItem(
        item = ChecklistOverview(
            id = 1,
            questionSetId = 1,
            name = "Inspection Checklist",
            progress = listOf(0.2f, 0.5f, 0.7f, 0f),
            createdOn = "15-03-26",
            lastModifiedOn = "15/03/25",
        ),
        onClick = { },
        onLongClick = { }
    )
}