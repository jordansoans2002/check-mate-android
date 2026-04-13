package com.man_behind.checkmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup


@Composable
fun GuidelineTooltip(
    text: String,
    onDismiss: () -> Unit,
){
    Popup(
        alignment = Alignment.TopStart,
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}