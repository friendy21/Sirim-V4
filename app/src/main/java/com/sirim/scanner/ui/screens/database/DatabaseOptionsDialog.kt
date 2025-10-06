package com.sirim.scanner.ui.screens.database

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DatabaseOptionsDialog(
    databaseName: String,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onScan: () -> Unit,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Storage,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = databaseName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Scanner Option (No admin required)
                DatabaseOption(
                    icon = Icons.Outlined.QrCodeScanner,
                    title = "Quick Scan",
                    description = "Scan SIRIM labels into this database",
                    onClick = {
                        onScan()
                        onDismiss()
                    }
                )

                // View Option (No admin required)
                DatabaseOption(
                    icon = Icons.Outlined.Visibility,
                    title = "View",
                    description = "View records, share, and export",
                    onClick = {
                        onView()
                        onDismiss()
                    }
                )

                // Edit Option (Admin required)
                DatabaseOption(
                    icon = Icons.Outlined.Edit,
                    title = "Edit",
                    description = "Modify records",
                    enabled = isAdmin,
                    onClick = {
                        onEdit()
                        onDismiss()
                    },
                    badge = if (!isAdmin) "Admin Required" else null
                )

                // Delete Option (Admin required)
                DatabaseOption(
                    icon = Icons.Outlined.Delete,
                    title = "Delete",
                    description = "Delete this database",
                    enabled = isAdmin,
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    badge = if (!isAdmin) "Admin Required" else null,
                    destructive = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatabaseOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    badge: String? = null,
    destructive: Boolean = false
) {
    Card(
        onClick = if (enabled) onClick else {{}},
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (destructive && enabled)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (destructive && enabled)
                    MaterialTheme.colorScheme.error
                else if (enabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (badge != null) {
                        AssistChip(
                            onClick = {},
                            label = { Text(badge, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
