package com.man_behind.checkmate.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.man_behind.checkmate.R


@Composable
fun ImageThumbnail(
    modifier: Modifier = Modifier,
    imageUri: Uri,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Box(
        modifier = modifier.size(84.dp)
    ) {
        AsyncImage(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onClick() },
            model = imageUri,
            placeholder = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = stringResource(R.string.attached_image_label),
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(12.dp),
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.remove_image_label),
                tint = Color.White,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ImageThumbnailPreview() {
    ImageThumbnail(
        imageUri = Uri.EMPTY,
        onClick = {  },
        onRemove = {  }
    )
}