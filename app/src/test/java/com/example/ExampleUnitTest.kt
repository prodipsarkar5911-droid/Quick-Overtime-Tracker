package com.example

import com.example.data.OvertimeEntry
import com.example.model.OvertimeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testOvertimeCalculation_RegularShift() {
        // 10:00 AM to 07:30 PM = 9h 30m shift (570 mins)
        // 1 hour break (60 mins) => 510 mins worked (< 540 mins, so 0 OT)
        val calc = OvertimeCalculator.calculateWorkAndOt(
            inTimeRaw = "10:00",
            inAmPm = "AM",
            outTimeRaw = "07:30",
            outAmPm = "PM",
            breakHr = "1 Hour",
            breakMin = "0 Min",
            isOffDay = false
        )

        assertNotNull(calc)
        assertEquals(510, calc!!.workMins) // 8h 30m
        assertEquals(0, calc.otMins)       // 0h OT
        assertEquals("8:30", OvertimeCalculator.minsToTimeStr(calc.workMins))
        assertEquals("0:00", OvertimeCalculator.minsToTimeStr(calc.otMins))
    }

    @Test
    fun testOvertimeCalculation_OvertimeShift() {
        // 10:00 AM to 10:00 PM = 12h shift (720 mins)
        // 1 hour break (60 mins) => 660 mins worked
        // 660 - 540 (9 hours) = 120 mins OT (2 hours)
        val calc = OvertimeCalculator.calculateWorkAndOt(
            inTimeRaw = "10:00",
            inAmPm = "AM",
            outTimeRaw = "10:00",
            outAmPm = "PM",
            breakHr = "1 Hour",
            breakMin = "0 Min",
            isOffDay = false
        )

        assertNotNull(calc)
        assertEquals(660, calc!!.workMins) // 11:00 hrs
        assertEquals(120, calc.otMins)      // 2:00 hrs
        assertEquals("11:00", OvertimeCalculator.minsToTimeStr(calc.workMins))
        assertEquals("2:00", OvertimeCalculator.minsToTimeStr(calc.otMins))
    }

    @Test
    fun testCsvExportAndImportRoundTrip() {
        val sampleEntries = listOf(
            OvertimeEntry(
                day = 1,
                inTime = "10:00",
                inAmPm = "AM",
                outTime = "07:30",
                outAmPm = "PM",
                breakHr = "1 Hour",
                breakMin = "0 Min",
                breakDisplay = "1h 0m",
                workMins = 510,
                otMins = 0,
                isOffDay = false
            ),
            OvertimeEntry(
                day = 2,
                inTime = "10:00",
                inAmPm = "AM",
                outTime = "10:00",
                outAmPm = "PM",
                breakHr = "1 Hour",
                breakMin = "0 Min",
                breakDisplay = "1h 0m",
                workMins = 660,
                otMins = 120,
                isOffDay = false
            ),
            OvertimeEntry(
                day = 3,
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

        val csv = OvertimeCalculator.generateCsv(sampleEntries)
        assertTrue(csv.contains("Day 1,10:00 AM,07:30 PM,1h 0m,8:30,0:00"))
        assertTrue(csv.contains("Day 2,10:00 AM,10:00 PM,1h 0m,11:00,2:00"))
        assertTrue(csv.contains("Day 3,OFF,OFF,0h 0m,0:00,0:00"))
        assertTrue(csv.contains("TOTAL,,,,19:30,2:00"))

        val importResult = OvertimeCalculator.parseCsv(csv)
        assertEquals(3, importResult.importedEntries.size)
        assertEquals(3, importResult.successCount)
        assertEquals(0, importResult.errorCount)

        val day2 = importResult.importedEntries.find { it.day == 2 }
        assertNotNull(day2)
        assertEquals(660, day2!!.workMins)
        assertEquals(120, day2.otMins)
    }
}
