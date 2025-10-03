package com.sirim.scanner.ui.screens.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.sirim.scanner.data.db.SirimRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordFormScreen(
    viewModel: RecordViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    recordId: Long? = null
) {
    val serialState = remember { mutableStateOf(TextFieldValue()) }
    val batchState = remember { mutableStateOf(TextFieldValue()) }
    val brandState = remember { mutableStateOf(TextFieldValue()) }
    val modelState = remember { mutableStateOf(TextFieldValue()) }
    val typeState = remember { mutableStateOf(TextFieldValue()) }
    val ratingState = remember { mutableStateOf(TextFieldValue()) }
    val sizeState = remember { mutableStateOf(TextFieldValue()) }

    LaunchedEffect(recordId) {
        recordId?.let { id ->
            viewModel.loadRecord(id)
        } ?: run {
            viewModel.resetActiveRecord()
            viewModel.clearFormError()
            serialState.value = TextFieldValue("")
            batchState.value = TextFieldValue("")
            brandState.value = TextFieldValue("")
            modelState.value = TextFieldValue("")
            typeState.value = TextFieldValue("")
            ratingState.value = TextFieldValue("")
            sizeState.value = TextFieldValue("")
        }
    }

    val activeRecord by viewModel.activeRecord.collectAsState()
    val formError by viewModel.formError.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()


    LaunchedEffect(activeRecord?.id) {
        activeRecord?.let { record ->
            serialState.value = TextFieldValue(record.sirimSerialNo)
            batchState.value = TextFieldValue(record.batchNo)
            brandState.value = TextFieldValue(record.brandTrademark)
            modelState.value = TextFieldValue(record.model)
            typeState.value = TextFieldValue(record.type)
            ratingState.value = TextFieldValue(record.rating)
            sizeState.value = TextFieldValue(record.size)
            viewModel.clearFormError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Form") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(
                value = serialState.value,
                onValueChange = {
                    serialState.value = it
                    viewModel.clearFormError()
                },
                label = { Text("SIRIM Serial No.") },
                isError = formError != null,
                supportingText = {
                    formError?.let { message ->
                        Text(text = message, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = batchState.value,
                onValueChange = { batchState.value = it },
                label = { Text("Batch No.") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = brandState.value,
                onValueChange = { brandState.value = it },
                label = { Text("Brand/Trademark") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = modelState.value,
                onValueChange = { modelState.value = it },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = typeState.value,
                onValueChange = { typeState.value = it },
                label = { Text("Type") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = ratingState.value,
                onValueChange = { ratingState.value = it },
                label = { Text("Rating") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = sizeState.value,
                onValueChange = { sizeState.value = it },
                label = { Text("Size") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (isSaving) return@Button
                    val record = activeRecord?.copy(
                        sirimSerialNo = serialState.value.text,
                        batchNo = batchState.value.text,
                        brandTrademark = brandState.value.text,
                        model = modelState.value.text,
                        type = typeState.value.text,
                        rating = ratingState.value.text,
                        size = sizeState.value.text
                    ) ?: SirimRecord(
                        sirimSerialNo = serialState.value.text,
                        batchNo = batchState.value.text,
                        brandTrademark = brandState.value.text,
                        model = modelState.value.text,
                        type = typeState.value.text,
                        rating = ratingState.value.text,
                        size = sizeState.value.text,
                        imagePath = null
                    )
                    viewModel.createOrUpdate(record) {
                        onSaved()
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Saving…")
                    }
                } else {
                    Text("Save Record")
                }
            }
        }
    }
}
