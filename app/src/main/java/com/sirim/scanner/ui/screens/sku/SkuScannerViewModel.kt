package com.sirim.scanner.ui.screens.sku

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sirim.scanner.data.db.SkuRecord
import com.sirim.scanner.data.ocr.BarcodeAnalyzer
import com.sirim.scanner.data.ocr.BarcodeDetection
import com.sirim.scanner.data.repository.SirimRepository
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SkuScannerViewModel private constructor(
    private val repository: SirimRepository,
    private val analyzer: BarcodeAnalyzer,
    private val appScope: CoroutineScope
) : ViewModel() {

    private val processing = AtomicBoolean(false)

    private val _status = MutableStateFlow(SkuScanStatus())
    val status: StateFlow<SkuScanStatus> = _status.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SkuScanStatus()
    )

    private val _batchMode = MutableStateFlow(false)
    private val _batchQueue = MutableStateFlow<List<SkuPendingRecord>>(emptyList())
    private val batchMutex = Mutex()

    val batchUiState: StateFlow<SkuBatchUiState> = combine(_batchMode, _batchQueue) { enabled, queue ->
        SkuBatchUiState(
            enabled = enabled,
            queued = queue.map { SkuBatchItem(it.barcode, it.format, it.capturedAt) }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SkuBatchUiState())

    private val _lastSavedId = MutableStateFlow<Long?>(null)
    val lastSavedId: StateFlow<Long?> = _lastSavedId

    private var lastDetectionValue: String? = null
    private var lastDetectionTimestamp: Long = 0L

    fun setBatchMode(enabled: Boolean) {
        if (_batchMode.value == enabled) return
        _batchMode.value = enabled
        _status.value = if (enabled) {
            SkuScanStatus(
                state = SkuScanState.Batch,
                message = "Batch mode enabled",
                barcode = null,
                format = null
            )
        } else {
            SkuScanStatus(message = "Batch mode disabled")
        }
    }

    fun clearBatchQueue() {
        viewModelScope.launch {
            val cleared = batchMutex.withLock {
                if (_batchQueue.value.isEmpty()) {
                    return@withLock false
                }
                _batchQueue.value = emptyList()
                true
            }
            if (cleared) {
                _status.value = SkuScanStatus(message = "Batch queue cleared")
            }
        }
    }

    fun saveBatch() {
        appScope.launch {
            val queued = batchMutex.withLock {
                if (_batchQueue.value.isEmpty()) {
                    return@withLock emptyList()
                }
                val snapshot = _batchQueue.value
                _batchQueue.value = emptyList()
                snapshot
            }
            if (queued.isEmpty()) {
                _status.value = _status.value.copy(message = "No queued barcodes to save")
                return@launch
            }
            val savedIds = mutableListOf<Long>()
            val skipped = mutableListOf<String>()
            queued.forEach { pending ->
                val existing = repository.findByBarcode(pending.barcode)
                if (existing != null) {
                    skipped += pending.barcode
                    return@forEach
                }
                val record = SkuRecord(barcode = pending.barcode)
                val id = repository.upsertSku(record)
                savedIds += id
            }
            _batchMode.value = false
            if (savedIds.isNotEmpty()) {
                _lastSavedId.value = savedIds.last()
            }
            val message = when {
                savedIds.isNotEmpty() && skipped.isNotEmpty() ->
                    "${savedIds.size} saved, ${skipped.size} duplicates skipped"
                savedIds.isNotEmpty() -> "Saved ${savedIds.size} barcodes"
                skipped.isNotEmpty() -> "Skipped duplicates: ${skipped.joinToString()}"
                else -> "Nothing saved"
            }
            _status.value = SkuScanStatus(
                state = if (skipped.isEmpty()) SkuScanState.Saved else SkuScanState.Duplicate,
                message = message
            )
        }
    }

    fun analyzeImage(imageProxy: ImageProxy) {
        if (!processing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        viewModelScope.launch {
            try {
                _status.value = _status.value.copy(state = SkuScanState.Scanning, message = "Scanning…")
                val detection = analyzer.analyze(imageProxy)
                if (detection == null) {
                    _status.value = SkuScanStatus()
                    return@launch
                }
                handleDetection(detection)
            } catch (error: Exception) {
                _status.value = SkuScanStatus(
                    state = SkuScanState.Error,
                    message = error.message ?: "Unable to read barcode"
                )
            } finally {
                imageProxy.close()
                processing.set(false)
            }
        }
    }

    private suspend fun handleDetection(detection: BarcodeDetection) {
        val normalized = detection.value.trim()
        if (normalized.isEmpty()) {
            _status.value = SkuScanStatus(state = SkuScanState.Error, message = "Detected barcode was empty")
            return
        }
        val now = System.currentTimeMillis()
        if (normalized == lastDetectionValue && (now - lastDetectionTimestamp) < DEDUP_WINDOW_MS) {
            return
        }
        lastDetectionValue = normalized
        lastDetectionTimestamp = now

        if (_batchMode.value) {
            val existing = repository.findByBarcode(normalized)
            if (existing != null) {
                _status.value = SkuScanStatus(
                    state = SkuScanState.Duplicate,
                    message = "Barcode already exists",
                    barcode = normalized,
                    format = detection.format
                )
                return
            }
            val queueSize = addToBatchQueue(
                SkuPendingRecord(
                    barcode = normalized,
                    format = detection.format,
                    capturedAt = now
                )
            )
            if (queueSize == null) {
                _status.value = SkuScanStatus(
                    state = SkuScanState.Batch,
                    message = "Barcode already queued",
                    barcode = normalized,
                    format = detection.format
                )
                return
            }
            _status.value = SkuScanStatus(
                state = SkuScanState.Batch,
                message = "Queued $queueSize barcodes",
                barcode = normalized,
                format = detection.format
            )
            return
        }

        val existing = repository.findByBarcode(normalized)
        if (existing != null) {
            _status.value = SkuScanStatus(
                state = SkuScanState.Duplicate,
                message = "Barcode already saved",
                barcode = normalized,
                format = detection.format
            )
            return
        }

        val record = SkuRecord(barcode = normalized)
        val id = repository.upsertSku(record)
        _lastSavedId.value = id
        _status.value = SkuScanStatus(
            state = SkuScanState.Saved,
            message = "Saved $normalized",
            barcode = normalized,
            format = detection.format
        )
    }

    companion object {
        private const val DEDUP_WINDOW_MS = 1_200L

        fun Factory(
            repository: SirimRepository,
            analyzer: BarcodeAnalyzer,
            appScope: CoroutineScope
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SkuScannerViewModel(repository, analyzer, appScope) as T
            }
        }
    }

    private suspend fun addToBatchQueue(pending: SkuPendingRecord): Int? = batchMutex.withLock {
        if (_batchQueue.value.any { it.barcode == pending.barcode }) {
            return@withLock null
        }
        val updated = _batchQueue.value + pending
        _batchQueue.value = updated
        updated.size
    }
}

data class SkuScanStatus(
    val state: SkuScanState = SkuScanState.Idle,
    val message: String = "Align a barcode within the guide",
    val barcode: String? = null,
    val format: String? = null
)

enum class SkuScanState {
    Idle,
    Scanning,
    Saved,
    Duplicate,
    Error,
    Batch
}

data class SkuBatchUiState(
    val enabled: Boolean = false,
    val queued: List<SkuBatchItem> = emptyList()
)

data class SkuBatchItem(
    val barcode: String,
    val format: String?,
    val capturedAt: Long
)

data class SkuPendingRecord(
    val barcode: String,
    val format: String?,
    val capturedAt: Long
)
