package com.sirim.scanner.ui.screens.sku

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sirim.scanner.data.db.DatabaseRecord
import com.sirim.scanner.data.db.SkuRecord
import com.sirim.scanner.data.preferences.PreferencesManager
import com.sirim.scanner.data.repository.SirimRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class DetailsFormState(
    val barcode: String = "",
    val capturedImage: Bitmap? = null,
    val productName: String = "",
    val category: String = "",
    val manufacturer: String = "",
    val description: String = "",
    val customFields: MutableMap<String, String> = mutableMapOf(),
    val additionalImage: Bitmap? = null,
    val isCreating: Boolean = false,
    val error: String? = null,
    val createdDatabaseId: Long? = null
)

class SkuDetailsViewModel(
    private val repository: SirimRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _formState = MutableStateFlow(DetailsFormState())
    val formState: StateFlow<DetailsFormState> = _formState.asStateFlow()

    fun initialize(barcode: String, image: Bitmap?) {
        _formState.value = _formState.value.copy(
            barcode = barcode,
            capturedImage = image
        )
    }

    fun updateProductName(name: String) {
        _formState.value = _formState.value.copy(productName = name, error = null)
    }

    fun updateCategory(category: String) {
        _formState.value = _formState.value.copy(category = category)
    }

    fun updateManufacturer(manufacturer: String) {
        _formState.value = _formState.value.copy(manufacturer = manufacturer)
    }

    fun updateDescription(description: String) {
        _formState.value = _formState.value.copy(description = description)
    }

    fun addCustomField(key: String, value: String) {
        val updatedFields = _formState.value.customFields.toMutableMap()
        updatedFields[key] = value
        _formState.value = _formState.value.copy(customFields = updatedFields)
    }

    fun removeCustomField(key: String) {
        val updatedFields = _formState.value.customFields.toMutableMap()
        updatedFields.remove(key)
        _formState.value = _formState.value.copy(customFields = updatedFields)
    }

    fun setAdditionalImage(image: Bitmap) {
        _formState.value = _formState.value.copy(additionalImage = image)
    }

    fun createDatabaseAndSku() {
        val state = _formState.value

        if (state.productName.isBlank()) {
            _formState.value = state.copy(error = "Product name is required")
            return
        }

        viewModelScope.launch {
            _formState.value = state.copy(isCreating = true, error = null)

            try {
                // Save captured image
                val imagePath = state.capturedImage?.let { bitmap ->
                    val bytes = ByteArrayOutputStream().use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        stream.toByteArray()
                    }
                    repository.persistImage(bytes, "jpg")
                }

                // Save additional image if provided
                val additionalImagePath = state.additionalImage?.let { bitmap ->
                    val bytes = ByteArrayOutputStream().use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        stream.toByteArray()
                    }
                    repository.persistImage(bytes, "jpg")
                }

                // Create custom fields JSON
                val customFieldsJson = if (state.customFields.isNotEmpty()) {
                    state.customFields.entries.joinToString(",") { "${it.key}:${it.value}" }
                } else null

                // Create SKU record
                val skuRecord = SkuRecord(
                    id = 0,
                    barcode = state.barcode,
                    productName = state.productName,
                    category = state.category.takeIf { it.isNotBlank() },
                    manufacturer = state.manufacturer.takeIf { it.isNotBlank() },
                    description = state.description.takeIf { it.isNotBlank() },
                    customFields = customFieldsJson,
                    batchNo = null,
                    brandTrademark = null,
                    model = null,
                    type = null,
                    rating = null,
                    size = null,
                    imagePath = imagePath,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isVerified = false,
                    needsSync = false,
                    serverId = null,
                    lastSynced = null
                )

                val skuId = repository.upsertSku(skuRecord)

                // Create database record
                val databaseRecord = DatabaseRecord(
                    id = 0,
                    name = state.productName,
                    filePath = "", // Will be set when exporting
                    thumbnailPath = imagePath,
                    recordCount = 0,
                    uniqueCount = 0,
                    duplicateCount = 0,
                    variantCount = 0,
                    fileSizeBytes = 0,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val databaseId = repository.insertDatabase(databaseRecord)

                // Set as active session
                preferencesManager.setLastActiveDatabaseId(databaseId)
                preferencesManager.setLastActiveSkuRecordId(skuId)
                preferencesManager.setFirstTimeLaunched()

                _formState.value = state.copy(
                    isCreating = false,
                    createdDatabaseId = databaseId
                )

            } catch (e: Exception) {
                _formState.value = state.copy(
                    isCreating = false,
                    error = "Failed to create database: ${e.message}"
                )
            }
        }
    }

    class Factory(
        private val repository: SirimRepository,
        private val preferencesManager: PreferencesManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SkuDetailsViewModel::class.java)) {
                return SkuDetailsViewModel(repository, preferencesManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
