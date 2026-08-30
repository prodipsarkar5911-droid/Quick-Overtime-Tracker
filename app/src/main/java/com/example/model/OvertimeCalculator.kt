package com.example.model

import com.example.data.OvertimeEntry
import java.util.Locale

object OvertimeCalculator {

    const val REGULAR_DUTY_MINUTES = 540 // 9 Hours = 540 mins

    fun parseBreakMinutes(breakHr: String, breakMin: String): Int {
        val hr = breakHr.filter { it.isDigit() }.toIntOrNull() ?: 0
        val min = breakMin.filter { it.isDigit() }.toIntOrNull() ?: 0
        return hr * 60 + min
    }

    fun parseTimeToMinutes(timeStr: String, amPm: String): Int? {
        val cleaned = timeStr.trim()
        if (cleaned.isEmpty()) return null

        val parts = cleaned.split(":")
        val rawHour = parts[0].toIntOrNull() ?: return null
        val rawMin = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0

        if (rawHour < 0 || rawHour > 23 || rawMin < 0 || rawMin > 59) {
            return null
        }

        var hour = rawHour
        val isPm = amPm.equals("PM", ignoreCase = true)
        val isAm = amPm.equals("AM", ignoreCase = true)

        if (isPm && hour < 12) {
            hour += 12
        } else if (isAm && hour == 12) {
            hour = 0
        }

        return (hour * 60 + rawMin) % (24 * 60)
    }

    data class CalculationResult(
        val workMins: Int,
        val otMins: Int,
        val formattedInTime: String,
        val formattedOutTime: String,
        val breakDisplay: String
    )

    fun calculateWorkAndOt(
        inTimeRaw: String,
        inAmPm: String,
        outTimeRaw: String,
        outAmPm: String,
        breakHr: String,
        breakMin: String,
        isOffDay: Boolean
    ): CalculationResult? {
        val hrVal = breakHr.filter { it.isDigit() }.toIntOrNull() ?: 0
        val minVal = breakMin.filter { it.isDigit() }.toIntOrNull() ?: 0
        val breakDisplay = "${hrVal}h ${minVal}m"

        if (isOffDay) {
            return CalculationResult(
                workMins = 0,
                otMins = 0,
                formattedInTime = "OFF",
                formattedOutTime = "OFF",
                breakDisplay = "0h 0m"
            )
        }

        val inMins = parseTimeToMinutes(inTimeRaw, inAmPm) ?: return null
        val outMins = parseTimeToMinutes(outTimeRaw, outAmPm) ?: return null

        var shiftMins = outMins - inMins
        if (shiftMins < 0) {
            shiftMins += 24 * 60 // crosses midnight
        }

        val breakMins = parseBreakMinutes(breakHr, breakMin)
        val actualWorkMins = (shiftMins - breakMins).coerceAtLeast(0)
        val otMins = (actualWorkMins - REGULAR_DUTY_MINUTES).coerceAtLeast(0)

        // format inTime as "HH:mm" or "H:mm"
        val formattedIn = formatTimeString(inTimeRaw)
        val formattedOut = formatTimeString(outTimeRaw)

        return CalculationResult(
            workMins = actualWorkMins,
            otMins = otMins,
            formattedInTime = formattedIn,
            formattedOutTime = formattedOut,
            breakDisplay = breakDisplay
        )
    }

    fun formatTimeString(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val parts = trimmed.split(":")
        val h = parts[0].toIntOrNull() ?: return raw
        val m = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    fun minsToTimeStr(totalMins: Int): String {
        val hrs = totalMins / 60
        val mins = totalMins % 60
        return String.format(Locale.US, "%d:%02d", hrs, mins)
    }

    fun generateCsv(entries: List<OvertimeEntry>): String {
        val sb = StringBuilder()
        sb.append("Day,Time In,Time Out,Break,Total Worked (H:MM),Overtime (H:MM)\n")
        var totalWorkMins = 0
        var totalOtMins = 0

        val sorted = entries.sortedBy { it.day }
        for (entry in sorted) {
            val inStr = if (entry.isOffDay || entry.inTime == "OFF") "OFF" else "${entry.inTime} ${entry.inAmPm}".trim()
            val outStr = if (entry.isOffDay || entry.outTime == "OFF") "OFF" else "${entry.outTime} ${entry.outAmPm}".trim()
            val breakStr = entry.breakDisplay.ifEmpty { "${entry.breakHr} ${entry.breakMin}" }
            val workStr = minsToTimeStr(entry.workMins)
            val otStr = minsToTimeStr(entry.otMins)

            sb.append("Day ${entry.day},$inStr,$outStr,$breakStr,$workStr,$otStr\n")
            totalWorkMins += entry.workMins
            totalOtMins += entry.otMins
        }

        sb.append("\nTOTAL,,,,${minsToTimeStr(totalWorkMins)},${minsToTimeStr(totalOtMins)}\n")
        return sb.toString()
    }

    data class ImportResult(
        val importedEntries: List<OvertimeEntry>,
        val successCount: Int,
        val errorCount: Int,
        val errorMessage: String? = null
    )

    fun parseCsv(csvContent: String): ImportResult {
        if (csvContent.isBlank()) {
            return ImportResult(emptyList(), 0, 0, "CSV content is empty")
        }

        val lines = csvContent.lines()
        val entries = mutableListOf<OvertimeEntry>()
        var errors = 0

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("Day,") || trimmed.startsWith("TOTAL", ignoreCase = true)) {
                continue
            }

            // Split by comma or semicolon
            val cols = parseCsvLine(trimmed)
            if (cols.isEmpty()) continue

            try {
                // Col 0: Day (e.g. "Day 1" or "1")
                val dayStr = cols[0].replace("Day", "", ignoreCase = true).trim()
                val dayNum = dayStr.toIntOrNull() ?: continue
                if (dayNum !in 1..31) continue

                val inStr = if (cols.size > 1) cols[1].trim() else ""
                val outStr = if (cols.size > 2) cols[2].trim() else ""
                val breakCol = if (cols.size > 3) cols[3].trim() else "1h 0m"

                val isOff = inStr.equals("OFF", ignoreCase = true) || outStr.equals("OFF", ignoreCase = true)

                if (isOff) {
                    entries.add(
                        OvertimeEntry(
                            day = dayNum,
                            inTime = "OFF",
                            inAmPm = "",
                            outTime = "OFF",
                            outAmPm = "",
                            breakHr = "0 Hour",
                            breakMin = "0 Min",
                            breakDisplay = "0h 0m",
                            workMins = 0,
                            otMins = 0,
                            isOffDay = true
                        )
                    )
                } else {
                    val (inTimeOnly, inAmPm) = splitTimeAndAmPm(inStr, "AM")
                    val (outTimeOnly, outAmPm) = splitTimeAndAmPm(outStr, "PM")

                    val (bHr, bMin, bDisplay) = parseBreakString(breakCol)

                    val calc = calculateWorkAndOt(
                        inTimeRaw = inTimeOnly,
                        inAmPm = inAmPm,
                        outTimeRaw = outTimeOnly,
                        outAmPm = outAmPm,
                        breakHr = bHr,
                        breakMin = bMin,
                        isOffDay = false
                    )

                    if (calc != null) {
                        entries.add(
                            OvertimeEntry(
                                day = dayNum,
                                inTime = calc.formattedInTime,
                                inAmPm = inAmPm,
                                outTime = calc.formattedOutTime,
                                outAmPm = outAmPm,
                                breakHr = bHr,
                                breakMin = bMin,
                                breakDisplay = bDisplay,
                                workMins = calc.workMins,
                                otMins = calc.otMins,
                                isOffDay = false
                            )
                        )
                    } else {
                        errors++
                    }
                }
            } catch (e: Exception) {
                errors++
            }
        }

        return ImportResult(
            importedEntries = entries,
            successCount = entries.size,
            errorCount = errors,
            errorMessage = if (entries.isEmpty()) "No valid overtime records found in the file." else null
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val current = StringBuilder()
        val delimiter = if (line.contains(";") && !line.contains(",")) ';' else ','

        for (ch in line) {
            when (ch) {
                '\"' -> inQuotes = !inQuotes
                delimiter -> {
                    if (inQuotes) {
                        current.append(ch)
                    } else {
                        result.add(current.toString().trim().removeSurrounding("\""))
                        current.clear()
                    }
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString().trim().removeSurrounding("\""))
        return result
    }

    private fun splitTimeAndAmPm(raw: String, defaultAmPm: String): Pair<String, String> {
        val upper = raw.uppercase().trim()
        val amPm = when {
            upper.endsWith("PM") -> "PM"
            upper.endsWith("AM") -> "AM"
            else -> defaultAmPm
        }
        val cleanTime = upper.replace("AM", "").replace("PM", "").trim()
        return Pair(cleanTime, amPm)
    }

    private fun parseBreakString(breakStr: String): Triple<String, String, String> {
        var hr = 0
        var min = 0
        val regex = Regex("(\\d+)\\s*h", RegexOption.IGNORE_CASE)
        val matchHr = regex.find(breakStr)
        if (matchHr != null) {
            hr = matchHr.groupValues[1].toIntOrNull() ?: 0
        }

        val minRegex = Regex("(\\d+)\\s*m", RegexOption.IGNORE_CASE)
        val matchMin = minRegex.find(breakStr)
        if (matchMin != null) {
            min = matchMin.groupValues[1].toIntOrNull() ?: 0
        }

        val hrStr = if (hr == 1) "1 Hour" else "$hr Hours"
        val minStr = if (min == 0) "0 Min" else "$min Mins"
        val display = "${hr}h ${min}m"
        return Triple(hrStr, minStr, display)
    }
}
