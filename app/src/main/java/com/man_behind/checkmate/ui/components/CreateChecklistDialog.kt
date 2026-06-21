package com.man_behind.checkmate.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.man_behind.checkmate.R
import com.man_behind.checkmate.data.model.QuestionSetOverview

@Composable
fun CreateChecklistDialog(
    questionSets: List<QuestionSetOverview>,
    onDismiss: () -> Unit,
    onCreate: (Long?, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedQuestionSetId by remember { mutableStateOf(
        if (questionSets.size == 1)
            questionSets[0].id
        else
            null
    ) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.create_checklist_popup_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.create_checklist_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 250.dp)
                ) {
                    items(questionSets) { questionSet ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedQuestionSetId = questionSet.id }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = questionSet.id == selectedQuestionSetId,
                                onClick = { selectedQuestionSetId = questionSet.id }
                            )
                            Text(text = questionSet.name)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        enabled = name.isNotBlank() && selectedQuestionSetId != null,
                        onClick = {
                            onCreate(selectedQuestionSetId, name.trim())
                        }
                    ) {
                        Text(stringResource(R.string.create))
                    }
                }
            }
        }
    }
}