package com.sirim.scanner.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SirimRecordDao {
    // ==================== BASIC QUERIES ====================
    
    @Query("SELECT * FROM sirim_records ORDER BY created_at DESC")
    fun getAllRecords(): Flow<List<SirimRecord>>

    @Query("SELECT * FROM sirim_records WHERE id = :id")
    suspend fun getRecordById(id: Long): SirimRecord?
    
    @Query("SELECT * FROM sirim_records WHERE sku_record_id = :skuRecordId ORDER BY created_at DESC")
    fun getRecordsBySkuId(skuRecordId: Long): Flow<List<SirimRecord>>
    
    @Query("SELECT * FROM sirim_records WHERE sku_record_id = :databaseId ORDER BY created_at DESC")
    fun getRecordsByDatabase(databaseId: Long): Flow<List<SirimRecord>>

    @Query(
        "SELECT * FROM sirim_records WHERE sirim_serial_no LIKE :query OR batch_no LIKE :query OR brand_trademark LIKE :query OR model LIKE :query ORDER BY created_at DESC"
    )
    fun searchRecords(query: String): Flow<List<SirimRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: SirimRecord): Long
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: SirimRecord): Long

    @Update
    suspend fun update(record: SirimRecord)

    @Delete
    suspend fun delete(record: SirimRecord)

    @Query("DELETE FROM sirim_records")
    suspend fun clearAll()
    
    // ==================== DUPLICATE DETECTION QUERIES ====================
    
    @Query("SELECT * FROM sirim_records WHERE sirim_serial_no = :serial LIMIT 1")
    suspend fun findBySerialExact(serial: String): SirimRecord?
    
    @Query("SELECT * FROM sirim_records WHERE sirim_serial_normalized = :normalized LIMIT 1")
    suspend fun findBySerialNormalized(normalized: String): SirimRecord?
    
    @Query("""
        SELECT * FROM sirim_records 
        WHERE LENGTH(sirim_serial_no) BETWEEN :minLength AND :maxLength
        ORDER BY id DESC
        LIMIT 100
    """)
    suspend fun findBySerialLengthRange(minLength: Int, maxLength: Int): List<SirimRecord>
    
    @Query("SELECT * FROM sirim_records WHERE sirim_serial_no = :serial")
    suspend fun findBySerial(serial: String): List<SirimRecord>
    
    @Query("""
        SELECT * FROM sirim_records 
        WHERE batch_no = :batchNo AND brand_trademark = :brand
        LIMIT 10
    """)
    suspend fun findByBatchAndBrand(batchNo: String, brand: String): List<SirimRecord>
    
    @Query("SELECT * FROM sirim_records WHERE content_hash = :hash LIMIT 1")
    suspend fun findByContentHash(hash: String): SirimRecord?
    
    @Query("SELECT * FROM sirim_records")
    suspend fun getAllSerials(): List<SirimRecord>
    
    @Query("SELECT sirim_serial_no FROM sirim_records")
    suspend fun getAllSerialNumbers(): List<String>
    
    // ==================== DUPLICATE STATUS QUERIES ====================
    
    @Query("""
        SELECT * FROM sirim_records 
        WHERE duplicate_status = :status
        ORDER BY created_at DESC
    """)
    suspend fun findByDuplicateStatus(status: String): List<SirimRecord>
    
    @Query("""
        SELECT COUNT(*) FROM sirim_records 
        WHERE duplicate_status = :status
    """)
    suspend fun countByDuplicateStatus(status: String): Int
    
    @Query("""
        UPDATE sirim_records 
        SET duplicate_status = :status,
            original_record_id = :originalId,
            duplicate_checked_at = :checkedAt,
            duplicate_confidence = :confidence,
            duplicate_reason = :reason,
            updated_at = :updatedAt
        WHERE id = :recordId
    """)
    suspend fun updateDuplicateStatus(
        recordId: Long,
        status: String,
        originalId: Long?,
        checkedAt: Long,
        confidence: Float?,
        reason: String?,
        updatedAt: Long = System.currentTimeMillis()
    )
    
    // ==================== STATISTICS ====================
    
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN duplicate_status = 'NEW' THEN 1 ELSE 0 END) as new_count,
            SUM(CASE WHEN duplicate_status = 'EXACT_DUPLICATE' THEN 1 ELSE 0 END) as duplicate_count,
            SUM(CASE WHEN duplicate_status = 'VARIANT' THEN 1 ELSE 0 END) as variant_count,
            SUM(CASE WHEN duplicate_status = 'POTENTIAL_DUPLICATE' THEN 1 ELSE 0 END) as potential_count
        FROM sirim_records
        WHERE sku_record_id = :skuRecordId
    """)
    suspend fun getStatistics(skuRecordId: Long): DuplicateStatistics
}

data class DuplicateStatistics(
    val total: Int,
    val new_count: Int,
    val duplicate_count: Int,
    val variant_count: Int,
    val potential_count: Int
)
