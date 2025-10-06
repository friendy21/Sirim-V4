package com.sirim.scanner.ui.screens.sku

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkuDetailsFormScreen(
    barcode: String,
    capturedImage: Bitmap?,
    onSave: (SkuDetails) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var productName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var manufacturer by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var additionalImageUri by remember { mutableStateOf<Uri?>(null) }
    var customFields by remember { mutableStateOf<List<CustomField>>(emptyList()) }
    var showAddFieldDialog by remember { mutableStateOf(false) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        additionalImageUri = uri
    }
    
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SKU Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onSave(
                                SkuDetails(
                                    barcode = barcode,
                                    productName = productName,
                                    category = category.takeIf { it.isNotBlank() },
                                    manufacturer = manufacturer.takeIf { it.isNotBlank() },
                                    description = description.takeIf { it.isNotBlank() },
                                    additionalImageUri = additionalImageUri,
                                    customFields = customFields
                                )
                            )
                        },
                        enabled = productName.isNotBlank()
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Barcode Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Barcode",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = barcode,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Required Fields Section
            Text(
                text = "Required Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("Product Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Optional Fields Section
            Text(
                text = "Optional Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = manufacturer,
                onValueChange = { manufacturer = it },
                label = { Text("Manufacturer") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Image Section
            Text(
                text = "Images",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Captured Image
                if (capturedImage != null) {
                    Card(
                        modifier = Modifier
                            .size(120.dp)
                    ) {
                        Image(
                            bitmap = capturedImage.asImageBitmap(),
                            contentDescription = "Captured image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Additional Image
                if (additionalImageUri != null) {
                    Card(
                        modifier = Modifier.size(120.dp)
                    ) {
                        AsyncImage(
                            model = additionalImageUri,
                            contentDescription = "Additional image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    OutlinedCard(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(120.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Add Image",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            // Custom Fields Section
            Text(
                text = "Custom Fields",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            customFields.forEachIndexed { index, field ->
                CustomFieldRow(
                    field = field,
                    onValueChange = { newValue ->
                        customFields = customFields.toMutableList().apply {
                            this[index] = field.copy(value = newValue)
                        }
                    },
                    onDelete = {
                        customFields = customFields.toMutableList().apply {
                            removeAt(index)
                        }
                    }
                )
            }

            OutlinedButton(
                onClick = { showAddFieldDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Custom Field")
            }

            // Help Text
            Text(
                text = "* Required fields. All other fields are optional and can be left blank.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showAddFieldDialog) {
        AddCustomFieldDialog(
            onDismiss = { showAddFieldDialog = false },
            onAdd = { fieldName, maxLength ->
                customFields = customFields + CustomField(
                    name = fieldName,
                    value = "",
                    maxLength = maxLength
                )
                showAddFieldDialog = false
            }
        )
    }
}

@Composable
private fun CustomFieldRow(
    field: CustomField,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = field.value,
            onValueChange = { if (it.length <= field.maxLength) onValueChange(it) },
            label = { Text(field.name) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            supportingText = {
                Text("${field.value.length}/${field.maxLength}")
            }
        )

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete field",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun AddCustomFieldDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Int) -> Unit
) {
    var fieldName by remember { mutableStateOf("") }
    var maxLength by remember { mutableStateOf("100") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Field") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = fieldName,
                    onValueChange = { fieldName = it },
                    label = { Text("Field Name") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = maxLength,
                    onValueChange = { maxLength = it.filter { char -> char.isDigit() } },
                    label = { Text("Max Length") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val length = maxLength.toIntOrNull() ?: 100
                    onAdd(fieldName, length)
                },
                enabled = fieldName.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

data class SkuDetails(
    val barcode: String,
    val productName: String,
    val category: String?,
    val manufacturer: String?,
    val description: String?,
    val additionalImageUri: Uri?,
    val customFields: List<CustomField>
)

data class CustomField(
    val name: String,
    val value: String,
    val maxLength: Int = 100
)
