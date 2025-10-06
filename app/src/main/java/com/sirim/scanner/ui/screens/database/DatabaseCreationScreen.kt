package com.sirim.scanner.ui.screens.database

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun DatabaseCreationScreen(
    databaseName: String,
    skuDetails: com.sirim.scanner.ui.screens.sku.SkuDetails,
    onCreated: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var creationState by remember { mutableStateOf<CreationState>(CreationState.Creating) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        // Simulate database creation process
        val steps = listOf(
            "Creating database structure..." to 0.2f,
            "Saving SKU record..." to 0.4f,
            "Generating indexes..." to 0.6f,
            "Creating Excel file..." to 0.8f,
            "Finalizing..." to 1.0f
        )

        for ((message, targetProgress) in steps) {
            creationState = CreationState.Creating(message)
            
            // Animate progress
            while (progress < targetProgress) {
                progress += 0.02f
                delay(50)
            }
            delay(300)
        }

        // Mark as complete
        creationState = CreationState.Success
        delay(1000)
        
        // Navigate to home with database ID
        onCreated(1L) // TODO: Return actual database ID
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val state = creationState) {
                is CreationState.Creating -> {
                    // Creating Animation
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(80.dp),
                        strokeWidth = 6.dp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Creating Database",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Database Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Storage,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Database Details",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Divider()

                            InfoRow("Name", databaseName)
                            InfoRow("First Product", skuDetails.productName)
                            InfoRow("Barcode", skuDetails.barcode)
                            if (skuDetails.category != null) {
                                InfoRow("Category", skuDetails.category)
                            }
                        }
                    }
                }

                CreationState.Success -> {
                    // Success Animation
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Database Created!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Your database has been successfully created. You can now start scanning SIRIM labels.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private sealed class CreationState {
    data class Creating(val message: String = "Initializing...") : CreationState()
    data object Success : CreationState()
}
