package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.OvertimeEntry
import com.example.data.OvertimeRepository
import com.example.model.OvertimeCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FormState(
    val selectedDay: Int = 1,
    val inTime: String = "10:00",
    val inAmPm: String = "AM",
    val outTime: String = "07:30",
    val outAmPm: String = "PM",
    val breakHr: String = "1 Hour",
    val breakMin: String = "0 Min",
    val isOffDay: Boolean = false,
    val errorMessage: String? = null,
    val userNotice: String? = null
)

class OvertimeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: OvertimeRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = OvertimeRepository(db.overtimeDao())
    }

    val allEntries: StateFlow<List<OvertimeEntry>> = repository.allEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _formState = MutableStateFlow(FormState())
    val formState: StateFlow<FormState> = _formState.asStateFlow()

    val totalWorkMins: StateFlow<Int> = allEntries.combine(_formState) { entries, _ ->
        entries.sumOf { it.workMins }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalOtMins: StateFlow<Int> = allEntries.combine(_formState) { entries, _ ->
        entries.sumOf { it.otMins }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val daysWorkedCount: StateFlow<Int> = allEntries.combine(_formState) { entries, _ ->
        entries.count { !it.isOffDay && it.inTime != "OFF" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val offDaysCount: StateFlow<Int> = allEntries.combine(_formState) { entries, _ ->
        entries.count { it.isOffDay || it.inTime == "OFF" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun selectDay(day: Int) {
        if (day !in 1..31) return
        val currentEntries = allEntries.value
        val existing = currentEntries.find { it.day == day }
        if (existing != null) {
            loadEntryIntoForm(existing)
        } else {
            _formState.value = _formState.value.copy(
                selectedDay = day,
                isOffDay = false,
                errorMessage = null,
                userNotice = null
            )
        }
    }

    fun loadEntryIntoForm(entry: OvertimeEntry) {
        if (entry.isOffDay || entry.inTime == "OFF") {
            _formState.value = _formState.value.copy(
                selectedDay = entry.day,
                isOffDay = true,
                inTime = "10:00",
                inAmPm = "AM",
                outTime = "07:30",
                outAmPm = "PM",
                breakHr = "0 Hour",
                breakMin = "0 Min",
                errorMessage = null,
                userNotice = null
            )
        } else {
            _formState.value = _formState.value.copy(
                selectedDay = entry.day,
                isOffDay = false,
                inTime = entry.inTime.ifEmpty { "10:00" },
                inAmPm = entry.inAmPm.ifEmpty { "AM" },
                outTime = entry.outTime.ifEmpty { "07:30" },
                outAmPm = entry.outAmPm.ifEmpty { "PM" },
                breakHr = entry.breakHr.ifEmpty { "1 Hour" },
                breakMin = entry.breakMin.ifEmpty { "0 Min" },
                errorMessage = null,
                userNotice = null
            )
        }
    }

    fun updateInTime(time: String) {
        _formState.value = _formState.value.copy(inTime = time, errorMessage = null)
    }

    fun updateInAmPm(amPm: String) {
        _formState.value = _formState.value.copy(inAmPm = amPm, errorMessage = null)
    }

    fun updateOutTime(time: String) {
        _formState.value = _formState.value.copy(outTime = time, errorMessage = null)
    }

    fun updateOutAmPm(amPm: String) {
        _formState.value = _formState.value.copy(outAmPm = amPm, errorMessage = null)
    }

    fun updateBreakHr(hr: String) {
        _formState.value = _formState.value.copy(breakHr = hr, errorMessage = null)
    }

    fun updateBreakMin(min: String) {
        _formState.value = _formState.value.copy(breakMin = min, errorMessage = null)
    }

    fun setOffDay(isOff: Boolean) {
        _formState.value = _formState.value.copy(
            isOffDay = isOff,
            errorMessage = null,
            breakHr = if (isOff) "0 Hour" else _formState.value.breakHr,
            breakMin = if (isOff) "0 Min" else _formState.value.breakMin
        )
    }

    fun clearNotice() {
        _formState.value = _formState.value.copy(userNotice = null, errorMessage = null)
    }

    fun saveCurrentEntry() {
        val state = _formState.value
        val day = state.selectedDay

        if (day !in 1..31) {
            _formState.value = state.copy(errorMessage = "Please select a valid day (1-31).")
            return
        }

        if (state.isOffDay) {
            val entry = OvertimeEntry(
                day = day,
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
            viewModelScope.launch {
                repository.saveEntry(entry)
                _formState.value = _formState.value.copy(
                    userNotice = "Day $day marked as Off Day / Sunday",
                    errorMessage = null
                )
            }
            return
        }

        if (state.inTime.isBlank() || state.outTime.isBlank()) {
            _formState.value = state.copy(errorMessage = "Please enter Time In and Time Out.")
            return
        }

        val calc = OvertimeCalculator.calculateWorkAndOt(
            inTimeRaw = state.inTime,
            inAmPm = state.inAmPm,
            outTimeRaw = state.outTime,
            outAmPm = state.outAmPm,
            breakHr = state.breakHr,
            breakMin = state.breakMin,
            isOffDay = false
        )

        if (calc == null) {
            _formState.value = state.copy(
                errorMessage = "Invalid time format. Please enter valid time (e.g. 10:00 or 10)."
            )
            return
        }

        val entry = OvertimeEntry(
            day = day,
            inTime = calc.formattedInTime,
            inAmPm = state.inAmPm,
            outTime = calc.formattedOutTime,
            outAmPm = state.outAmPm,
            breakHr = state.breakHr,
            breakMin = state.breakMin,
            breakDisplay = calc.breakDisplay,
            workMins = calc.workMins,
            otMins = calc.otMins,
            isOffDay = false
        )

        viewModelScope.launch {
            repository.saveEntry(entry)
            val otStr = OvertimeCalculator.minsToTimeStr(calc.otMins)
            val workStr = OvertimeCalculator.minsToTimeStr(calc.workMins)
            _formState.value = _formState.value.copy(
                userNotice = "Day $day saved! Worked: $workStr hrs (OT: $otStr hrs)",
                errorMessage = null
            )
        }
    }

    fun deleteEntry(day: Int) {
        viewModelScope.launch {
            repository.deleteEntry(day)
            _formState.value = _formState.value.copy(
                userNotice = "Day $day record deleted.",
                errorMessage = null
            )
        }
    }

    fun clearAllMonth() {
        viewModelScope.launch {
            repository.clearAll()
            _formState.value = _formState.value.copy(
                userNotice = "All monthly overtime data cleared.",
                errorMessage = null
            )
        }
    }

    fun importCsvContent(csvString: String) {
        val result = OvertimeCalculator.parseCsv(csvString)
        if (result.importedEntries.isNotEmpty()) {
            viewModelScope.launch {
                repository.saveAll(result.importedEntries)
                _formState.value = _formState.value.copy(
                    userNotice = "Successfully imported ${result.successCount} daily records!" +
                            (if (result.errorCount > 0) " (${result.errorCount} skipped)" else ""),
                    errorMessage = null
                )
            }
        } else {
            _formState.value = _formState.value.copy(
                errorMessage = result.errorMessage ?: "Failed to import CSV. Check format."
            )
        }
    }

    fun getExportCsvContent(): String {
        return OvertimeCalculator.generateCsv(allEntries.value)
    }
}
