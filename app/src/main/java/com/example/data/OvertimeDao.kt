package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OvertimeDao {
    @Query("SELECT * FROM overtime_entries ORDER BY day ASC")
    fun getAllEntries(): Flow<List<OvertimeEntry>>

    @Query("SELECT * FROM overtime_entries WHERE day = :day LIMIT 1")
    fun getEntryByDay(day: Int): Flow<OvertimeEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: OvertimeEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<OvertimeEntry>)

    @Query("DELETE FROM overtime_entries WHERE day = :day")
    suspend fun deleteByDay(day: Int)

    @Query("DELETE FROM overtime_entries")
    suspend fun clearAll()
}
