package com.sirim.scanner.ui.screens.sku

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PreviewState(
    val capturedImage: Bitmap? = null,
    val detectedBarcode: String? = null,
    val barcodeFormat: String? = null
)

class SkuPreviewViewModel : ViewModel() {

    private val _previewState = MutableStateFlow(PreviewState())
    val previewState: StateFlow<PreviewState> = _previewState.asStateFlow()

    fun setPreviewData(image: Bitmap, barcode: String, format: String) {
        _previewState.value = PreviewState(
            capturedImage = image,
            detectedBarcode = barcode,
            barcodeFormat = format
        )
    }

    fun clearPreview() {
        _previewState.value = PreviewState()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SkuPreviewViewModel::class.java)) {
                return SkuPreviewViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
