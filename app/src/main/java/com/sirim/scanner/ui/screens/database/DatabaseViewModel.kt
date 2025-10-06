package com.sirim.scanner.ui.screens.database

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sirim.scanner.data.db.DatabaseRecord
import com.sirim.scanner.data.db.SirimRecord
import com.sirim.scanner.data.duplicate.DuplicateType
import com.sirim.scanner.data.repository.SirimRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DatabaseViewModel(
    private val repository: SirimRepository
) : ViewModel() {

    private val _databases = MutableStateFlow<List<DatabaseRecord>>(emptyList())
    val databases: StateFlow<List<DatabaseRecord>> = _databases.asStateFlow()

    private val _currentDatabase = MutableStateFlow<DatabaseRecord?>(null)
    val currentDatabase: StateFlow<DatabaseRecord?> = _currentDatabase.asStateFlow()

    private val _records = MutableStateFlow<List<SirimRecord>>(emptyList())
    val records: StateFlow<List<SirimRecord>> = _records.asStateFlow()

    private val _statistics = MutableStateFlow(DatabaseStatistics(0, 0, 0, 0))
    val statistics: StateFlow<DatabaseStatistics> = _statistics.asStateFlow()

    init {
        loadDatabases()
    }

    private fun loadDatabases() {
        viewModelScope.launch {
            repository.getAllDatabases().collect { databaseList ->
                _databases.value = databaseList
            }
        }
    }

    fun loadDatabase(databaseId: Long) {
        viewModelScope.launch {
            // Load database details
            repository.getDatabaseById(databaseId)?.let { database ->
                _currentDatabase.value = database
            }

            // Load records for this database
            repository.getRecordsByDatabase(databaseId).collect { recordList ->
                _records.value = recordList
                updateStatistics(recordList)
            }
        }
    }

    private fun updateStatistics(records: List<SirimRecord>) {
        val total = records.size
        val unique = records.count { it.duplicateStatus == DuplicateType.NEW.name }
        val duplicates = records.count { it.duplicateStatus == DuplicateType.EXACT_DUPLICATE.name }
        val variants = records.count { it.duplicateStatus == DuplicateType.VARIANT.name }

        _statistics.value = DatabaseStatistics(
            total = total,
            unique = unique,
            duplicates = duplicates,
            variants = variants
        )
    }

    fun createDatabase(name: String, skuBarcode: String, productName: String) {
        viewModelScope.launch {
            val database = DatabaseRecord(
                id = 0,
                name = name,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                recordCount = 0,
                uniqueCount = 0,
                duplicateCount = 0,
                filePath = "",
                fileSizeBytes = 0
            )
            repository.insertDatabase(database)
        }
    }

    fun deleteDatabase(databaseId: Long) {
        viewModelScope.launch {
            repository.deleteDatabase(databaseId)
        }
    }

    class Factory(
        private val repository: SirimRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DatabaseViewModel::class.java)) {
                return DatabaseViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
