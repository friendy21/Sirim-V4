package com.sirim.scanner.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DatabaseRecordDao {
    @Query("SELECT * FROM databases ORDER BY created_at DESC")
    fun getAllDatabases(): Flow<List<DatabaseRecord>>

    @Query("SELECT * FROM databases WHERE id = :id")
    suspend fun getDatabaseById(id: Long): DatabaseRecord?
    
    @Query("SELECT * FROM databases WHERE name = :name LIMIT 1")
    suspend fun getDatabaseByName(name: String): DatabaseRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(database: DatabaseRecord): Long

    @Update
    suspend fun update(database: DatabaseRecord)

    @Delete
    suspend fun delete(database: DatabaseRecord)

    @Query("DELETE FROM databases")
    suspend fun clearAll()
}
