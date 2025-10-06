package com.sirim.scanner.ui.screens.database

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sirim.scanner.data.db.SirimRecord
import com.sirim.scanner.data.duplicate.DuplicateType
import com.sirim.scanner.ui.screens.duplicate.DuplicateStatusIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseViewScreen(
    databaseName: String,
    records: List<SirimRecord>,
    statistics: DatabaseStatistics,
    isEditMode: Boolean,
    onBack: () -> Unit,
    onQuickScan: () -> Unit,
    onExport: () -> Unit,
    onRecordClick: (SirimRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var filterStatus by remember { mutableStateOf<DuplicateType?>(null) }

    val filteredRecords = remember(records, filterStatus) {
        if (filterStatus == null) records
        else records.filter { it.duplicateStatus == filterStatus.name }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(databaseName)
                        Text(
                            text = "${records.size} records",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onQuickScan) {
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Quick Scan")
                    }

                    IconButton(onClick = onExport) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = "Export")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Statistics Card
            DatabaseStatisticsCard(
                statistics = statistics,
                modifier = Modifier.padding(16.dp)
            )

            // Status Filter
            StatusFilterRow(
                selectedStatus = filterStatus,
                onStatusSelected = { filterStatus = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Records List
            if (filteredRecords.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No records found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredRecords) { record ->
                        RecordRow(
                            record = record,
                            onClick = { onRecordClick(record) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DatabaseStatisticsCard(
    statistics: DatabaseStatistics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Total", statistics.total.toString(), Icons.Outlined.Inventory)
                StatItem("Unique", statistics.unique.toString(), Icons.Outlined.CheckCircle)
                StatItem("Duplicates", statistics.duplicates.toString(), Icons.Outlined.ContentCopy)
                StatItem("Variants", statistics.variants.toString(), Icons.Outlined.Info)
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun StatusFilterRow(
    selectedStatus: DuplicateType?,
    onStatusSelected: (DuplicateType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedStatus == null,
            onClick = { onStatusSelected(null) },
            label = { Text("All") }
        )

        DuplicateType.values().forEach { type ->
            FilterChip(
                selected = selectedStatus == type,
                onClick = { onStatusSelected(type) },
                label = { Text("${type.emoji} ${type.displayName}") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordRow(
    record: SirimRecord,
    onClick: () -> Unit
) {
    val duplicateType = DuplicateType.valueOf(record.duplicateStatus)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator
            DuplicateStatusIcon(duplicateType, size = 24.dp)

            // Record Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.sirimSerialNo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                if (record.brandTrademark != null) {
                    Text(
                        text = record.brandTrademark,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Confidence Badge
            if (record.duplicateConfidence != null && duplicateType != DuplicateType.NEW) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            "${(record.duplicateConfidence * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}

data class DatabaseStatistics(
    val total: Int,
    val unique: Int,
    val duplicates: Int,
    val variants: Int
)
