package com.man_behind.checkmate.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
        modifier = modifier
            .size(84.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            modifier = Modifier.matchParentSize(),
            model = imageUri,
            placeholder = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = stringResource(R.string.attached_image_label),
            contentScale = ContentScale.Crop,
        )

        IconButton(
            modifier = Modifier
                .size(8.dp)
                .align(Alignment.TopEnd)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                ),
            onClick = onRemove,

        ) {
            Icon(
                modifier = Modifier.align(Alignment.Center),
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