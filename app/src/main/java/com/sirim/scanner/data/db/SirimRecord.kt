package com.sirim.scanner.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sirim_records",
    foreignKeys = [
        ForeignKey(
            entity = SkuRecord::class,
            parentColumns = ["id"],
            childColumns = ["sku_record_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sirim_serial_no"]),
        Index(value = ["sirim_serial_normalized"]),
        Index(value = ["content_hash"]),
        Index(value = ["batch_no", "brand_trademark"]),
        Index(value = ["duplicate_status"]),
        Index(value = ["sku_record_id"]),
        Index(value = ["created_at"]),
        Index(value = ["is_verified"])
    ]
)
data class SirimRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "sku_record_id")
    val skuRecordId: Long,
    
    @ColumnInfo(name = "sirim_serial_no")
    val sirimSerialNo: String,
    
    // Normalized serial for faster matching
    @ColumnInfo(name = "sirim_serial_normalized")
    val sirimSerialNormalized: String,
    
    @ColumnInfo(name = "batch_no")
    val batchNo: String?,
    
    @ColumnInfo(name = "brand_trademark")
    val brandTrademark: String?,
    
    @ColumnInfo(name = "model")
    val model: String?,
    
    @ColumnInfo(name = "type")
    val type: String?,
    
    @ColumnInfo(name = "rating")
    val rating: String?,
    
    @ColumnInfo(name = "size")
    val size: String?,
    
    // Content hash for duplicate detection
    @ColumnInfo(name = "content_hash")
    val contentHash: String,
    
    // Duplicate detection fields
    @ColumnInfo(name = "duplicate_status")
    val duplicateStatus: String = "NEW", // NEW, EXACT_DUPLICATE, POTENTIAL_DUPLICATE, VARIANT
    
    @ColumnInfo(name = "original_record_id")
    val originalRecordId: Long? = null,
    
    @ColumnInfo(name = "duplicate_checked_at")
    val duplicateCheckedAt: Long? = null,
    
    @ColumnInfo(name = "duplicate_confidence")
    val duplicateConfidence: Float? = null,
    
    @ColumnInfo(name = "duplicate_reason")
    val duplicateReason: String? = null,
    
    @ColumnInfo(name = "image_path")
    val imagePath: String?,
    
    @ColumnInfo(name = "qr_payload")
    val qrPayload: String?,
    
    @ColumnInfo(name = "ocr_confidence")
    val ocrConfidence: Float?,
    
    @ColumnInfo(name = "verification_status")
    val verificationStatus: String?, // QR_OCR_MATCH, QR_ONLY, OCR_ONLY, CONFLICT
    
    @ColumnInfo(name = "custom_fields")
    val customFields: String?, // JSON
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean = false,
    
    @ColumnInfo(name = "needs_sync")
    val needsSync: Boolean = true,
    
    @ColumnInfo(name = "server_id")
    val serverId: String? = null,
    
    @ColumnInfo(name = "last_synced")
    val lastSynced: Long? = null
)
