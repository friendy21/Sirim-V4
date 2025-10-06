package com.sirim.scanner.data.ocr

import android.graphics.Rect
import java.util.Locale
import kotlin.math.min

/**
 * Improved SIRIM Label Parser with enhanced accuracy for T+9 digit serial numbers.
 * Optimized for the specific SIRIM label format shown in reference images.
 */
object SirimLabelParser {

    // Precise pattern for SIRIM serial numbers: T followed by exactly 9 digits
    private val SIRIM_SERIAL_EXACT = Regex("T\\d{9}")
    
    // Relaxed pattern for serial with spaces/dashes
    private val SIRIM_SERIAL_RELAXED = Regex("T[\\s\\-]?(\\d{9})")
    
    // Legacy TEA format (7 digits)
    private val TEA_SERIAL_PATTERN = Regex("TEA[\\s\\-]?(\\d{7})")

    // Character corrections for common OCR mistakes
    private val characterCorrections = mapOf(
        'O' to '0',
        'o' to '0',
        'I' to '1',
        'l' to '1',
        'i' to '1',
        'S' to '5',
        's' to '5',
        'B' to '8',
        'Z' to '2',
        'z' to '2',
        'G' to '6',
        'D' to '0'
    )

    // Improved field patterns with better specificity
    private val fieldPatterns = mapOf(
        "batchNo" to listOf(
            Regex("(?:Batch|BATCH)\\s*(?:No\\.?|Number)?\\s*[:\\-]?\\s*([A-Z0-9\\-]{3,200})", RegexOption.IGNORE_CASE),
            Regex("\\b([A-Z]{2,4}-\\d{4,})\\b") // SAO-025105 format
        ),
        "brandTrademark" to listOf(
            Regex("(?:Brand|BRAND)\\s*/\\s*(?:Trademark|TRADEMARK)\\s*[:\\-]?\\s*([A-Z0-9][A-Z0-9 &'\\-]{1,1023})", RegexOption.IGNORE_CASE),
            Regex("(?:Brand|BRAND|Trademark|TRADEMARK)\\s*[:\\-]?\\s*([A-Z0-9][A-Z0-9 &'\\-]{1,1023})", RegexOption.IGNORE_CASE)
        ),
        "model" to listOf(
            Regex("(?:Model|MODEL)\\s*[:\\-]?\\s*([A-Za-z0-9][A-Za-z0-9\\-\\s/]{1,1499})", RegexOption.IGNORE_CASE)
        ),
        "type" to listOf(
            Regex("(?:Type|TYPE)\\s*[:\\-]?\\s*([A-Za-z0-9][A-Za-z0-9\\-\\s/]{1,1499})", RegexOption.IGNORE_CASE)
        ),
        "rating" to listOf(
            Regex("(?:Rating|RATING)\\s*[:\\-]?\\s*([A-Za-z0-9][A-Za-z0-9\\-\\s/]{0,599})", RegexOption.IGNORE_CASE),
            Regex("\\b(SAE\\s*\\d{1,2}W-?\\d{1,2})\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(API\\s*[A-Z]{2}(?:-\\d+)?)\\b", RegexOption.IGNORE_CASE)
        ),
        "size" to listOf(
            Regex("(?:Size|SIZE)\\s*[:\\-]?\\s*(\\d+\\s*(?:L|ML|LITRE|LTR|KG))\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(\\d+\\s*(?:L|ML|LITRE|LTR|KG))\\b", RegexOption.IGNORE_CASE)
        )
    )

    /**
     * Parse OCR text and extract SIRIM label fields.
     */
    fun parse(text: String): Map<String, FieldConfidence> {
        if (text.isBlank()) return emptyMap()

        val normalized = normalizeText(text)
        val candidates = mutableMapOf<String, FieldConfidence>()

        // Extract serial number
        extractSerial(normalized)?.let { serial ->
            candidates["sirimSerialNo"] = serial
        }

        // Extract other fields
        fieldPatterns.forEach { (field, patterns) ->
            patterns.forEachIndexed { index, pattern ->
                val match = pattern.find(normalized)
                if (match != null) {
                    val raw = match.groupValues.lastOrNull()?.trim() ?: ""
                    if (raw.isNotBlank()) {
                        val candidate = buildCandidate(field, raw, primaryPattern = index == 0)
                        candidates.merge(field, candidate) { old, new ->
                            if (new.confidence > old.confidence) new else old
                        }
                    }
                }
            }
        }

        return candidates
    }

    /**
     * Merge QR code data with OCR results for verification.
     */
    fun mergeWithQr(
        current: MutableMap<String, FieldConfidence>,
        qrPayload: String?
    ): FieldConfidence? {
        val qrSerial = qrPayload?.let { payload ->
            // Extract T+9 digits from QR
            val match = SIRIM_SERIAL_EXACT.find(payload)
            match?.value?.uppercase(Locale.getDefault())
        } ?: return null

        val corrected = correctCharacters(qrSerial)
        val qrConfidence = FieldConfidence(
            value = corrected.first,
            confidence = 0.98f, // Very high confidence for QR
            source = FieldSource.QR,
            notes = corrected.second
        )

        val existing = current["sirimSerialNo"]
        if (existing != null) {
            // Verify QR matches OCR
            if (existing.value.equals(qrSerial, ignoreCase = true)) {
                // Perfect match - highest confidence
                val verified = qrConfidence.copy(
                    confidence = 0.99f,
                    notes = qrConfidence.notes + FieldNote.VERIFIED_BY_MULTIPLE_SOURCES
                )
                current["sirimSerialNo"] = verified
                return verified
            } else {
                // Mismatch - prefer QR but flag it
                val flagged = qrConfidence.copy(
                    confidence = 0.90f,
                    notes = qrConfidence.notes + FieldNote.CONFLICTING_SOURCES
                )
                current["sirimSerialNo"] = flagged
                return flagged
            }
        }
        
        current["sirimSerialNo"] = qrConfidence
        return qrConfidence
    }

    /**
     * Extract serial number from text using multiple strategies.
     */
    private fun extractSerial(text: String): FieldConfidence? {
        // Strategy 1: Exact pattern (T + 9 digits, no spaces)
        SIRIM_SERIAL_EXACT.find(text)?.let { match ->
            val (corrected, notes) = correctCharacters(match.value)
            return FieldConfidence(
                value = corrected,
                confidence = 0.90f, // High confidence for exact match
                source = FieldSource.OCR,
                notes = notes
            )
        }

        // Strategy 2: Relaxed pattern (T + 9 digits with spaces/dashes)
        SIRIM_SERIAL_RELAXED.find(text)?.let { match ->
            val digits = match.groupValues[1]
            val cleaned = "T$digits"
            val (corrected, notes) = correctCharacters(cleaned)
            return FieldConfidence(
                value = corrected,
                confidence = 0.85f,
                source = FieldSource.OCR,
                notes = notes + FieldNote.PATTERN_RELAXED
            )
        }

        // Strategy 3: Legacy TEA format
        TEA_SERIAL_PATTERN.find(text)?.let { match ->
            val digits = match.groupValues[1]
            val cleaned = "TEA$digits"
            val (corrected, notes) = correctCharacters(cleaned)
            return FieldConfidence(
                value = corrected,
                confidence = 0.78f,
                source = FieldSource.OCR,
                notes = notes + FieldNote.PATTERN_RELAXED
            )
        }

        return null
    }

    /**
     * Build field candidate with confidence scoring.
     */
    private fun buildCandidate(
        field: String,
        rawValue: String,
        primaryPattern: Boolean
    ): FieldConfidence {
        val (corrected, notes) = correctCharacters(rawValue)
        val (trimmed, wasTrimmed) = enforceLength(field, corrected)
        
        val baseConfidence = if (primaryPattern) 0.75f else 0.65f
        val lengthPenalty = if (wasTrimmed) 0.05f else 0f
        val correctionPenalty = notes.size * 0.03f
        
        val confidence = (baseConfidence - lengthPenalty - correctionPenalty)
            .coerceIn(0.1f, 0.95f)
        
        val allNotes = notes.toMutableSet()
        if (wasTrimmed) allNotes.add(FieldNote.LENGTH_TRIMMED)
        if (!primaryPattern) allNotes.add(FieldNote.PATTERN_RELAXED)
        
        return FieldConfidence(
            value = trimmed,
            confidence = confidence,
            source = FieldSource.OCR,
            notes = allNotes
        )
    }

    /**
     * Enforce maximum field lengths.
     */
    private fun enforceLength(field: String, value: String): Pair<String, Boolean> {
        val limit = when (field) {
            "sirimSerialNo" -> 12
            "batchNo" -> 200
            "brandTrademark" -> 1024
            "model", "type", "size" -> 1500
            "rating" -> 600
            else -> 512
        }
        return if (value.length > limit) {
            value.substring(0, min(limit, value.length)) to true
        } else {
            value to false
        }
    }

    /**
     * Correct common OCR character mistakes.
     */
    private fun correctCharacters(value: String): Pair<String, Set<FieldNote>> {
        var corrected = value
        val appliedNotes = mutableSetOf<FieldNote>()
        
        characterCorrections.forEach { (wrong, correct) ->
            if (corrected.contains(wrong)) {
                corrected = corrected.replace(wrong, correct)
                appliedNotes += FieldNote.CORRECTED_CHARACTER
            }
        }
        
        return corrected to appliedNotes
    }

    /**
     * Normalize text for better pattern matching.
     */
    private fun normalizeText(text: String): String {
        return text
            .replace("\uFF1A", ":") // Full-width colon
            .replace(Regex("\\s+"), " ") // Multiple spaces
            .trim()
    }

    /**
     * Convert field key to human-readable label.
     */
    fun prettifyKey(key: String): String {
        return when (key) {
            "sirimSerialNo" -> "SIRIM Serial No."
            "batchNo" -> "Batch No."
            "brandTrademark" -> "Brand/Trademark"
            "model" -> "Model"
            "type" -> "Type"
            "rating" -> "Rating"
            "size" -> "Size"
            else -> key.replace(Regex("([A-Z])")) { " ${it.value}" }
                .trim()
                .replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) 
                    else it.toString() 
                }
        }
    }

    /**
     * Validate serial number format.
     */
    fun isValidSerialFormat(serial: String): Boolean {
        return SIRIM_SERIAL_EXACT.matches(serial) || TEA_SERIAL_PATTERN.matches(serial)
    }
}
