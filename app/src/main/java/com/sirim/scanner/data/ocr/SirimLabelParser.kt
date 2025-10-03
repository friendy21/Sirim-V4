package com.sirim.scanner.data.ocr

import java.util.Locale
import kotlin.math.min

object SirimLabelParser {

    private val characterCorrections = mapOf(
        'O' to '0',
        'o' to '0',
        'I' to '1',
        'l' to '1',
        'S' to '5',
        's' to '5',
        'B' to '8'
    )

    private val serialPatterns = listOf(
        Regex("\\bTEA[-\\s]?([0-9O]{7})\\b", RegexOption.IGNORE_CASE),
        Regex("\\bT[-\\s]?([0-9O]{9})\\b", RegexOption.IGNORE_CASE),
        Regex("\\bSerial\\s*(No|Number)?[:\\uFF1A\\-]?\\s*([A-Z0-9]{6,12})\\b", RegexOption.IGNORE_CASE)
    )

    private val fieldPatterns = mapOf(
        "batchNo" to listOf(
            Regex("Batch\\s*(No|Number)?\\.?\\s*[:\\uFF1A\\-]?\\s*([A-Z0-9-]{5,200})", RegexOption.IGNORE_CASE),
            Regex("\\b([A-Z]{2,}-\\d{4,})\\b")
        ),
        "brandTrademark" to listOf(
            Regex("Brand\\s*/?\\s*Trademark\\s*[:\\uFF1A\\-]?\\s*([A-Z0-9 &'\\-]{2,1024})", RegexOption.IGNORE_CASE)
        ),
        "model" to listOf(
            Regex("Model\\s*[:\\uFF1A\\-]?\\s*([A-Za-z0-9\\- /]{2,1500})", RegexOption.IGNORE_CASE)
        ),
        "type" to listOf(
            Regex("Type\\s*[:\\uFF1A\\-]?\\s*([A-Za-z0-9\\- /]{2,1500})", RegexOption.IGNORE_CASE)
        ),
        "rating" to listOf(
            Regex("Rating\\s*[:\\uFF1A\\-]?\\s*([A-Za-z0-9\\- /]{1,600})", RegexOption.IGNORE_CASE),
            Regex("SAE\\s*([0-9]{1,2}W-?[0-9]{1,2})", RegexOption.IGNORE_CASE)
        ),
        "size" to listOf(
            Regex("Size\\s*[:\\uFF1A\\-]?\\s*([0-9]+\\s*(L|ML|LITRE|LTR|KG))", RegexOption.IGNORE_CASE),
            Regex("([0-9]+\\s*(L|ML|LITRE|LTR|KG))", RegexOption.IGNORE_CASE)
        )
    )

    fun parse(text: String): Map<String, FieldConfidence> {
        if (text.isBlank()) return emptyMap()

        val normalized = collapseVerticalText(text)
        val candidates = mutableMapOf<String, FieldConfidence>()

        extractSerial(normalized)?.let { serial ->
            candidates["sirimSerialNo"] = serial
        }

        fieldPatterns.forEach { (field, patterns) ->
            patterns.forEachIndexed { index, pattern ->
                val match = pattern.find(normalized)
                if (match != null) {
                    val raw = match.groupValues.last().trim()
                    val candidate = buildCandidate(field, raw, primaryPattern = index == 0)
                    candidates.merge(field, candidate) { old, new -> old.mergeWith(new) }
                }
            }
        }

        return candidates
    }

    fun mergeWithQr(
        current: MutableMap<String, FieldConfidence>,
        qrPayload: String?
    ): FieldConfidence? {
        val qrSerial = qrPayload?.let { payload ->
            val match = Regex("T[0-9]{9}", RegexOption.IGNORE_CASE).find(payload)
            match?.value?.uppercase(Locale.getDefault())
        } ?: return null

        val corrected = correctCharacters(qrSerial)
        val qrConfidence = FieldConfidence(
            value = corrected.first,
            confidence = 0.95f,
            source = FieldSource.QR,
            notes = corrected.second
        )

        val existing = current["sirimSerialNo"]
        if (existing != null) {
            val merged = existing.mergeWith(qrConfidence)
            current["sirimSerialNo"] = merged
            return merged
        }
        current["sirimSerialNo"] = qrConfidence
        return qrConfidence
    }

    fun prettifyKey(key: String): String = key.replace(Regex("([A-Z])")) {
        " " + it.value.lowercase(Locale.getDefault())
    }.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    private fun extractSerial(text: String): FieldConfidence? {
        serialPatterns.forEachIndexed { index, regex ->
            val match = regex.find(text)
            if (match != null) {
                val raw = match.groupValues.last().trim()
                val (corrected, notes) = correctCharacters(raw)
                val normalized = corrected.uppercase(Locale.getDefault())
                val confidenceBoost = if (index == 0) 0.35f else 0.2f
                val confidence = 0.55f + confidenceBoost - 0.05f * notes.count()
                return FieldConfidence(
                    value = normalized,
                    confidence = confidence.coerceIn(0.1f, 0.95f),
                    source = FieldSource.OCR,
                    notes = notes
                )
            }
        }
        return null
    }

    private fun buildCandidate(
        field: String,
        rawValue: String,
        primaryPattern: Boolean
    ): FieldConfidence {
        val (corrected, notes) = correctCharacters(rawValue)
        val trimmed = enforceLength(field, corrected)
        val baseConfidence = if (primaryPattern) 0.55f else 0.45f
        val lengthPenalty = if (trimmed.second) 0.05f else 0f
        val notesSet = notes + if (trimmed.second) setOf(FieldNote.LENGTH_TRIMMED) else emptySet()
        val patternRelaxed = if (!primaryPattern) setOf(FieldNote.PATTERN_RELAXED) else emptySet()
        val confidence = (baseConfidence - lengthPenalty - notes.count() * 0.05f)
            .coerceIn(0.1f, 0.9f)
        return FieldConfidence(
            value = trimmed.first,
            confidence = confidence,
            source = FieldSource.OCR,
            notes = notesSet + patternRelaxed
        )
    }

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

    private fun correctCharacters(value: String): Pair<String, Set<FieldNote>> {
        var corrected = value
        val appliedNotes = mutableSetOf<FieldNote>()
        characterCorrections.forEach { (wrong, correct) ->
            if (corrected.indexOf(wrong, ignoreCase = false) >= 0) {
                corrected = corrected.replace(wrong, correct)
                appliedNotes += FieldNote.CORRECTED_CHARACTER
            }
        }
        return corrected to appliedNotes
    }

    private fun collapseVerticalText(text: String): String {
        val cleaned = text.replace("\uFF1A", ":")
        val lines = cleaned.lines()
        val verticalSegments = buildString {
            var buffer = StringBuilder()
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.length == 1 && trimmed.firstOrNull()?.isLetterOrDigit() == true) {
                    buffer.append(trimmed)
                } else {
                    if (buffer.length >= 4) {
                        append(buffer)
                        append(' ')
                    }
                    buffer = StringBuilder()
                }
            }
            if (buffer.length >= 4) {
                append(buffer)
            }
        }.trim()

        return "$cleaned $verticalSegments"
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
