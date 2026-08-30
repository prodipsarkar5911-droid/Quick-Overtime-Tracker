package com.example.data

import kotlinx.coroutines.flow.Flow

class OvertimeRepository(private val dao: OvertimeDao) {
    val allEntries: Flow<List<OvertimeEntry>> = dao.getAllEntries()

    fun getEntryByDay(day: Int): Flow<OvertimeEntry?> = dao.getEntryByDay(day)

    suspend fun saveEntry(entry: OvertimeEntry) = dao.insertOrUpdate(entry)

    suspend fun saveAll(entries: List<OvertimeEntry>) = dao.insertAll(entries)

    suspend fun deleteEntry(day: Int) = dao.deleteByDay(day)

    suspend fun clearAll() = dao.clearAll()
}
