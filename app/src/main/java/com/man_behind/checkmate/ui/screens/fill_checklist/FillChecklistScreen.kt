package com.man_behind.checkmate.ui.screens.fill_checklist

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.man_behind.checkmate.data.model.Checklist
import com.man_behind.checkmate.ui.components.ImageSourcePicker
import java.time.LocalDateTime

@Composable
fun FillChecklistScreen(
    viewModel: FillChecklistViewModel = hiltViewModel(),
) {

    val checklist by viewModel.checklist.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    // Handles app background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.flushAll()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.flushAll() // navigate away
        }
    }

    val uiState by viewModel.state.collectAsState()

    val imagePicker = ImageSourcePicker(
        {}, {}, {}
    )

    FillChecklistContent(uiState)
}


@Composable
fun FillChecklistContent(
    uiState: FillChecklistUiState
) {

}


@Preview(showBackground = true)
@Composable
fun FillChecklistContentPreview() {
    FillChecklistContent(
        uiState = FillChecklistUiState(
            Checklist(
                id = 1,
                name = "Inspection Checklist",
                sections = emptyList(),
                createdOn = LocalDateTime.of(2021, 3, 14, 0, 0),
            )
        )
    )
}