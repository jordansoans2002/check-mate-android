package com.man_behind.checkmate.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.man_behind.checkmate.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSourcePicker(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                modifier = Modifier.padding(bottom = 16.dp),
                text = stringResource(R.string.add_image_label),
                style = MaterialTheme.typography.titleLarge,
            )

            ListItem(
                modifier = Modifier.clickable { onGalleryClick() },
                headlineContent = { Text("Gallery") },
                leadingContent = { Icon(Icons.Default.PhotoLibrary, null) },
            )

            ListItem(
                modifier = Modifier.clickable { onCameraClick() },
                headlineContent = { Text("Camera") },
                leadingContent = { Icon(Icons.Default.AddToPhotos,null)}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ImageSourcePickerPreview() {
    ImageSourcePicker(
        {}, {}, {}
    )
}
