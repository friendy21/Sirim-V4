package com.sirim.scanner.data.duplicate

import com.sirim.scanner.data.db.SirimRecord

/**
 * Configuration for duplicate detection
 */
data class DetectionConfig(
    val enableNormalizedMatch: Boolean = true,
    val enableFuzzyMatch: Boolean = true,
    val enableCompositeMatch: Boolean = true,
    val enableHashMatch: Boolean = true,
    val fuzzyThreshold: Int = 2,
    val timeoutMs: Long = 100
)

/**
 * Result of duplicate detection
 */
data class DuplicateResult(
    val type: DuplicateType,
    val confidence: Float, // 0.0 to 1.0
    val matchedRecord: SirimRecord?,
    val matchReason: String,
    val suggestions: List<String> = emptyList()
) {
    val isNew: Boolean get() = type == DuplicateType.NEW
    val isDuplicate: Boolean get() = type != DuplicateType.NEW
    val confidencePercentage: Int get() = (confidence * 100).toInt()
}

/**
 * Types of duplicate detection results
 */
enum class DuplicateType {
    NEW,                    // No match found - completely new item
    EXACT_DUPLICATE,        // 100% match - identical serial number
    POTENTIAL_DUPLICATE,    // Similar but not identical - possible OCR error
    VARIANT;                // Same product, different batch/variant
    
    val displayName: String
        get() = when (this) {
            NEW -> "New Item"
            EXACT_DUPLICATE -> "Exact Duplicate"
            POTENTIAL_DUPLICATE -> "Potential Duplicate"
            VARIANT -> "Variant"
        }
    
    val emoji: String
        get() = when (this) {
            NEW -> "🟢"
            EXACT_DUPLICATE -> "🔴"
            POTENTIAL_DUPLICATE -> "🟠"
            VARIANT -> "🟡"
        }
}
