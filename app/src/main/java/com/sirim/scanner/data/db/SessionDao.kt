package com.sirim.scanner.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE user_id = :userId LIMIT 1")
    suspend fun getSessionByUserId(userId: Long): Session?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: Session): Long

    @Update
    suspend fun update(session: Session)
    
    @Query("DELETE FROM sessions WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: Long)
}
