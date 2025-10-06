package com.sirim.scanner.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sku_records",
    indices = [
        Index(value = ["barcode"]),
        Index(value = ["product_name"]),
        Index(value = ["created_at"]),
        Index(value = ["brand_trademark"]),
        Index(value = ["is_verified"])
    ]
)
data class SkuRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "barcode")
    val barcode: String,
    
    @ColumnInfo(name = "product_name")
    val productName: String,
    
    @ColumnInfo(name = "category")
    val category: String? = null,
    
    @ColumnInfo(name = "manufacturer")
    val manufacturer: String? = null,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "batch_no")
    val batchNo: String? = null,
    
    @ColumnInfo(name = "brand_trademark")
    val brandTrademark: String? = null,
    
    @ColumnInfo(name = "model")
    val model: String? = null,
    
    @ColumnInfo(name = "type")
    val type: String? = null,
    
    @ColumnInfo(name = "rating")
    val rating: String? = null,
    
    @ColumnInfo(name = "size")
    val size: String? = null,
    
    @ColumnInfo(name = "image_path")
    val imagePath: String?,
    
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
