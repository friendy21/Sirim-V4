package com.sirim.scanner.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "databases",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["created_at"])
    ]
)
data class DatabaseRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "file_path")
    val filePath: String,
    
    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String?,
    
    @ColumnInfo(name = "record_count")
    val recordCount: Int = 0,
    
    @ColumnInfo(name = "unique_count")
    val uniqueCount: Int = 0,
    
    @ColumnInfo(name = "duplicate_count")
    val duplicateCount: Int = 0,
    
    @ColumnInfo(name = "variant_count")
    val variantCount: Int = 0,
    
    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long = 0,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
