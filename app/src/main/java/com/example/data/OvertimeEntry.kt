package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "overtime_entries")
data class OvertimeEntry(
    @PrimaryKey val day: Int, // Day of month: 1 to 31
    val inTime: String = "",   // e.g. "10:00" or "OFF"
    val inAmPm: String = "AM", // "AM" or "PM"
    val outTime: String = "",  // e.g. "07:30" or "OFF"
    val outAmPm: String = "PM",// "PM" or "AM"
    val breakHr: String = "1 Hour",
    val breakMin: String = "0 Min",
    val breakDisplay: String = "1h 0m",
    val workMins: Int = 0,
    val otMins: Int = 0,
    val isOffDay: Boolean = false,
    val note: String = ""
)
