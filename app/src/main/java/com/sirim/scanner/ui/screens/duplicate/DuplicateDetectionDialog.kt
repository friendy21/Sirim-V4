package com.sirim.scanner.ui.screens.duplicate

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sirim.scanner.data.duplicate.DuplicateResult
import com.sirim.scanner.data.duplicate.DuplicateType

@Composable
fun DuplicateDetectionDialog(
    result: DuplicateResult,
    onDismiss: () -> Unit,
    onSaveAnyway: () -> Unit,
    onViewOriginal: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            DuplicateStatusIcon(result.type, size = 48.dp)
        },
        title = {
            Text(
                text = result.type.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Confidence Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (result.type) {
                            DuplicateType.EXACT_DUPLICATE -> MaterialTheme.colorScheme.errorContainer
                            DuplicateType.POTENTIAL_DUPLICATE -> MaterialTheme.colorScheme.tertiaryContainer
                            DuplicateType.VARIANT -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Confidence",
                            style = MaterialTheme.typography.labelMedium
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { result.confidence },
                                modifier = Modifier.weight(1f),
                            )

                            Text(
                                text = "${result.confidencePercentage}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Match Reason
                Text(
                    text = result.matchReason,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Suggestions
                if (result.suggestions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Suggestions:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        result.suggestions.forEach { suggestion ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Original Record Info
                if (result.matchedRecord != null) {
                    HorizontalDivider()

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Original Record",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Serial: ${result.matchedRecord.sirimSerialNo}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (result.matchedRecord.batchNo != null) {
                            Text(
                                text = "Batch: ${result.matchedRecord.batchNo}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (result.matchedRecord != null) {
                    TextButton(onClick = onViewOriginal) {
                        Text("View Original")
                    }
                }

                TextButton(onClick = onSaveAnyway) {
                    Text("Save Anyway")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DuplicateStatusIcon(
    type: DuplicateType,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    val (icon, tint) = when (type) {
        DuplicateType.NEW -> Icons.Outlined.CheckCircle to MaterialTheme.colorScheme.primary
        DuplicateType.EXACT_DUPLICATE -> Icons.Outlined.Error to MaterialTheme.colorScheme.error
        DuplicateType.POTENTIAL_DUPLICATE -> Icons.Outlined.Warning to MaterialTheme.colorScheme.tertiary
        DuplicateType.VARIANT -> Icons.Outlined.Info to MaterialTheme.colorScheme.secondary
    }

    Icon(
        imageVector = icon,
        contentDescription = type.displayName,
        modifier = Modifier.size(size),
        tint = tint
    )
}
