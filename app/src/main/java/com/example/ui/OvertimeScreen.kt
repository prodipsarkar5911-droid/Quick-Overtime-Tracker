package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.OvertimeEntry
import com.example.model.OvertimeCalculator
import com.example.ui.components.AppTimePickerDialog
import com.example.ui.theme.OffDaySlate
import com.example.ui.theme.OffDaySlateContainer
import com.example.ui.theme.OvertimeAmber
import com.example.ui.theme.OvertimeAmberContainer
import com.example.ui.theme.WorkedGreen
import com.example.ui.theme.WorkedGreenContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OvertimeScreen(
    viewModel: OvertimeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val entries by viewModel.allEntries.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val totalWorkMins by viewModel.totalWorkMins.collectAsStateWithLifecycle()
    val totalOtMins by viewModel.totalOtMins.collectAsStateWithLifecycle()
    val daysWorked by viewModel.daysWorkedCount.collectAsStateWithLifecycle()
    val offDays by viewModel.offDaysCount.collectAsStateWithLifecycle()

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showTimeInPicker by remember { mutableStateOf(false) }
    var showTimeOutPicker by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var generatedCsvContent by remember { mutableStateOf("") }

    // Activity Result Launcher for Importing CSV
    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BufferedReader(InputStreamReader(stream)).readText()
                        } ?: ""
                    }
                    if (content.isNotBlank()) {
                        viewModel.importCsvContent(content)
                    } else {
                        Toast.makeText(context, "Could not read CSV file", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error importing CSV: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Activity Result Launcher for Saving CSV file
    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val csvData = viewModel.getExportCsvContent()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            OutputStreamWriter(stream).use { writer ->
                                writer.write(csvData)
                            }
                        }
                    }
                    Toast.makeText(context, "CSV exported successfully!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error saving CSV: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Show transient toast/notice
    LaunchedEffect(formState.userNotice) {
        formState.userNotice?.let { notice ->
            Toast.makeText(context, notice, Toast.LENGTH_SHORT).show()
            viewModel.clearNotice()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Quick Overtime Tracker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AM/PM & Overtime Focus",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(
                        onClick = {
                            importCsvLauncher.launch("text/*")
                        },
                        modifier = Modifier.testTag("import_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Import CSV",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            generatedCsvContent = viewModel.getExportCsvContent()
                            showExportDialog = true
                        },
                        modifier = Modifier.testTag("export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export CSV",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.testTag("clear_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear All Month",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Monthly Summary Card
            item {
                SummaryCard(
                    totalWorkMins = totalWorkMins,
                    totalOtMins = totalOtMins,
                    daysWorked = daysWorked,
                    offDays = offDays
                )
            }

            // 2. Quick Day Picker (1-31)
            item {
                QuickDayPickerCard(
                    selectedDay = formState.selectedDay,
                    entries = entries,
                    onDaySelected = { day ->
                        focusManager.clearFocus()
                        viewModel.selectDay(day)
                    }
                )
            }

            // 3. Daily Entry Form
            item {
                DailyEntryCard(
                    formState = formState,
                    entries = entries,
                    onInTimeChange = { viewModel.updateInTime(it) },
                    onInAmPmChange = { viewModel.updateInAmPm(it) },
                    onOutTimeChange = { viewModel.updateOutTime(it) },
                    onOutAmPmChange = { viewModel.updateOutAmPm(it) },
                    onBreakHrChange = { viewModel.updateBreakHr(it) },
                    onBreakMinChange = { viewModel.updateBreakMin(it) },
                    onOffDayChange = { viewModel.setOffDay(it) },
                    onOpenTimeInPicker = { showTimeInPicker = true },
                    onOpenTimeOutPicker = { showTimeOutPicker = true },
                    onSave = {
                        focusManager.clearFocus()
                        viewModel.saveCurrentEntry()
                    },
                    onDeleteDay = {
                        focusManager.clearFocus()
                        viewModel.deleteEntry(formState.selectedDay)
                    },
                    onPrevDay = {
                        if (formState.selectedDay > 1) {
                            viewModel.selectDay(formState.selectedDay - 1)
                        }
                    },
                    onNextDay = {
                        if (formState.selectedDay < 31) {
                            viewModel.selectDay(formState.selectedDay + 1)
                        }
                    }
                )
            }

            // 4. Action Buttons Bar (Export, Import, Clear)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            generatedCsvContent = viewModel.getExportCsvContent()
                            showExportDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_export_csv")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV", fontSize = 13.sp)
                    }

                    FilledTonalButton(
                        onClick = {
                            importCsvLauncher.launch("text/*")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_import_csv")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import CSV", fontSize = 13.sp)
                    }
                }
            }

            // 5. Entries Records Table / List
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly Records (${entries.size}/31)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (entries.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearConfirmDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    EmptyRecordsCard(
                        onSelectDay = { viewModel.selectDay(1) },
                        onImportClick = { importCsvLauncher.launch("text/*") }
                    )
                }
            } else {
                item {
                    // Table Header
                    TableHeaderRow()
                }

                items(entries, key = { it.day }) { entry ->
                    TableRowCard(
                        entry = entry,
                        isSelected = entry.day == formState.selectedDay,
                        onClick = { viewModel.loadEntryIntoForm(entry) },
                        onDelete = { viewModel.deleteEntry(entry.day) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Time In Picker Dialog
    if (showTimeInPicker) {
        val (h, m) = parseHourAndMin(formState.inTime, formState.inAmPm)
        AppTimePickerDialog(
            title = "Select Time In",
            initialHour = h,
            initialMinute = m,
            onConfirm = { hour12, minute, amPm ->
                viewModel.updateInTime(String.format(Locale.US, "%02d:%02d", hour12, minute))
                viewModel.updateInAmPm(amPm)
                showTimeInPicker = false
            },
            onDismiss = { showTimeInPicker = false }
        )
    }

    // Time Out Picker Dialog
    if (showTimeOutPicker) {
        val (h, m) = parseHourAndMin(formState.outTime, formState.outAmPm)
        AppTimePickerDialog(
            title = "Select Time Out",
            initialHour = h,
            initialMinute = m,
            onConfirm = { hour12, minute, amPm ->
                viewModel.updateOutTime(String.format(Locale.US, "%02d:%02d", hour12, minute))
                viewModel.updateOutAmPm(amPm)
                showTimeOutPicker = false
            },
            onDismiss = { showTimeOutPicker = false }
        )
    }

    // Clear Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear All Month Data?") },
            text = { Text("Are you sure you want to reset and clear all overtime records for this month? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllMonth()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            icon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Export Overtime CSV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose how you want to export your overtime data (Total ${entries.size} days):")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = generatedCsvContent.take(200) + if (generatedCsvContent.length > 200) "..." else "",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            shareCsvText(context, generatedCsvContent)
                            showExportDialog = false
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                    Button(
                        onClick = {
                            saveCsvLauncher.launch("overtime_data.csv")
                            showExportDialog = false
                        }
                    ) {
                        Text("Save to File")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun parseHourAndMin(timeStr: String, amPm: String): Pair<Int, Int> {
    val parts = timeStr.trim().split(":")
    val rawHour = parts[0].toIntOrNull() ?: 10
    val minute = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
    var hour24 = rawHour
    if (amPm.equals("PM", ignoreCase = true) && hour24 < 12) {
        hour24 += 12
    } else if (amPm.equals("AM", ignoreCase = true) && hour24 == 12) {
        hour24 = 0
    }
    return Pair(hour24, minute)
}

private fun shareCsvText(context: Context, csvText: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, csvText)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Overtime CSV")
    context.startActivity(shareIntent)
}

@Composable
fun SummaryCard(
    totalWorkMins: Int,
    totalOtMins: Int,
    daysWorked: Int,
    offDays: Int
) {
    val totalWorkStr = OvertimeCalculator.minsToTimeStr(totalWorkMins)
    val totalOtStr = OvertimeCalculator.minsToTimeStr(totalOtMins)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("summary_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Standard: 9h/day",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Worked Box
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = WorkedGreenContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = WorkedGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Total Worked",
                                style = MaterialTheme.typography.labelMedium,
                                color = WorkedGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "$totalWorkStr hrs",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = WorkedGreen
                        )
                    }
                }

                // Total Overtime Box
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = OvertimeAmberContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = OvertimeAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Total OT",
                                style = MaterialTheme.typography.labelMedium,
                                color = OvertimeAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "$totalOtStr hrs",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = OvertimeAmber
                        )
                    }
                }
            }

            // Secondary Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = "Worked: $daysWorked Days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Off / Sundays: $offDays Days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun QuickDayPickerCard(
    selectedDay: Int,
    entries: List<OvertimeEntry>,
    onDaySelected: (Int) -> Unit
) {
    val entriesMap = remember(entries) { entries.associateBy { it.day } }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_day_picker_card"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Day Selector (1-31)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Day $selectedDay selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Horizontal scrollable day chips or 2 rows
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (day in 1..31) {
                    val entry = entriesMap[day]
                    val isSelected = day == selectedDay
                    val isOff = entry?.isOffDay == true || entry?.inTime == "OFF"
                    val hasOt = (entry?.otMins ?: 0) > 0
                    val hasWorked = entry != null && !isOff

                    val containerColor = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        hasOt -> OvertimeAmberContainer
                        hasWorked -> WorkedGreenContainer
                        isOff -> OffDaySlateContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                    val textColor = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        hasOt -> OvertimeAmber
                        hasWorked -> WorkedGreen
                        isOff -> OffDaySlate
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(containerColor)
                            .clickable { onDaySelected(day) }
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$day",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected || entry != null) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                            if (hasOt) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else OvertimeAmber)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DailyEntryCard(
    formState: FormState,
    entries: List<OvertimeEntry>,
    onInTimeChange: (String) -> Unit,
    onInAmPmChange: (String) -> Unit,
    onOutTimeChange: (String) -> Unit,
    onOutAmPmChange: (String) -> Unit,
    onBreakHrChange: (String) -> Unit,
    onBreakMinChange: (String) -> Unit,
    onOffDayChange: (Boolean) -> Unit,
    onOpenTimeInPicker: () -> Unit,
    onOpenTimeOutPicker: () -> Unit,
    onSave: () -> Unit,
    onDeleteDay: () -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit
) {
    val existingEntry = remember(entries, formState.selectedDay) {
        entries.find { it.day == formState.selectedDay }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_entry_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Selected Day Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevDay,
                    enabled = formState.selectedDay > 1,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Entry: Day ${formState.selectedDay}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (existingEntry != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Saved",
                                tint = WorkedGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onNextDay,
                    enabled = formState.selectedDay < 31,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Off Day Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOffDayChange(!formState.isOffDay) }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = formState.isOffDay,
                    onCheckedChange = { onOffDayChange(it) },
                    modifier = Modifier.testTag("off_day_checkbox")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Mark as Off Day / Sunday",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Zero work hours & zero overtime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = !formState.isOffDay) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Time In Field + AM/PM Toggle + TimePicker Icon
                    TimeInputRow(
                        label = "Time In (e.g. 10:00 or 10)",
                        timeValue = formState.inTime,
                        amPmValue = formState.inAmPm,
                        onTimeChange = onInTimeChange,
                        onAmPmChange = onInAmPmChange,
                        onOpenPicker = onOpenTimeInPicker,
                        timeTag = "time_in_input",
                        amPmTag = "time_in_ampm"
                    )

                    // Time Out Field + AM/PM Toggle + TimePicker Icon
                    TimeInputRow(
                        label = "Time Out (e.g. 07:30 or 19:30)",
                        timeValue = formState.outTime,
                        amPmValue = formState.outAmPm,
                        onTimeChange = onOutTimeChange,
                        onAmPmChange = onOutAmPmChange,
                        onOpenPicker = onOpenTimeOutPicker,
                        timeTag = "time_out_input",
                        amPmTag = "time_out_ampm"
                    )

                    // Break Duration
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Break Duration",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Break Hours Chips
                        Text(
                            text = "Hours:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("0 Hour", "1 Hour", "2 Hours", "3 Hours").forEach { hr ->
                                val selected = formState.breakHr == hr
                                FilterChip(
                                    selected = selected,
                                    onClick = { onBreakHrChange(hr) },
                                    label = { Text(hr, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Break Minutes Chips
                        Text(
                            text = "Minutes:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("0 Min", "15 Mins", "30 Mins", "45 Mins").forEach { min ->
                                val selected = formState.breakMin == min
                                FilterChip(
                                    selected = selected,
                                    onClick = { onBreakMinChange(min) },
                                    label = { Text(min, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Live preview of calculation
                    val liveCalc = OvertimeCalculator.calculateWorkAndOt(
                        inTimeRaw = formState.inTime,
                        inAmPm = formState.inAmPm,
                        outTimeRaw = formState.outTime,
                        outAmPm = formState.outAmPm,
                        breakHr = formState.breakHr,
                        breakMin = formState.breakMin,
                        isOffDay = false
                    )

                    if (liveCalc != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Estimated Work: ${OvertimeCalculator.minsToTimeStr(liveCalc.workMins)} hrs",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "OT: ${OvertimeCalculator.minsToTimeStr(liveCalc.otMins)} hrs",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (liveCalc.otMins > 0) OvertimeAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Error display
            if (formState.errorMessage != null) {
                Text(
                    text = formState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Save / Update & Delete Day buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_update_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (existingEntry != null) "Update Day ${formState.selectedDay}" else "Save Day ${formState.selectedDay}",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (existingEntry != null) {
                    OutlinedButton(
                        onClick = onDeleteDay,
                        modifier = Modifier.testTag("delete_day_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete entry",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeInputRow(
    label: String,
    timeValue: String,
    amPmValue: String,
    onTimeChange: (String) -> Unit,
    onAmPmChange: (String) -> Unit,
    onOpenPicker: () -> Unit,
    timeTag: String,
    amPmTag: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = timeValue,
                onValueChange = onTimeChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag(timeTag),
                singleLine = true,
                placeholder = { Text("e.g. 10:00") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    IconButton(onClick = onOpenPicker) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Pick Time",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp)
            )

            // AM / PM Segmented Toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(2.dp)
                    .testTag(amPmTag)
            ) {
                listOf("AM", "PM").forEach { ap ->
                    val isSelected = amPmValue.equals(ap, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { onAmPmChange(ap) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ap,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TableHeaderRow() {
    Surface(
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Day",
                modifier = Modifier.weight(1.1f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "In / Out",
                modifier = Modifier.weight(2f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Break",
                modifier = Modifier.weight(1.1f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Worked",
                modifier = Modifier.weight(1.4f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "OT",
                modifier = Modifier.weight(1.2f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun TableRowCard(
    entry: OvertimeEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isOff = entry.isOffDay || entry.inTime == "OFF"
    val hasOt = entry.otMins > 0
    val workStr = OvertimeCalculator.minsToTimeStr(entry.workMins)
    val otStr = OvertimeCalculator.minsToTimeStr(entry.otMins)

    val inDisplay = if (isOff) "OFF" else "${entry.inTime} ${entry.inAmPm}".trim()
    val outDisplay = if (isOff) "OFF" else "${entry.outTime} ${entry.outAmPm}".trim()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("row_day_${entry.day}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                isOff -> OffDaySlateContainer.copy(alpha = 0.4f)
                hasOt -> OvertimeAmberContainer.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day
            Text(
                text = "Day ${entry.day}",
                modifier = Modifier.weight(1.1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )

            // In / Out
            Column(modifier = Modifier.weight(2f)) {
                if (isOff) {
                    Text(
                        text = "OFF DAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = OffDaySlate,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = inDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = outDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Break
            Text(
                text = if (isOff) "-" else entry.breakDisplay.ifEmpty { "0h 0m" },
                modifier = Modifier.weight(1.1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Worked Hours
            Text(
                text = if (isOff) "0:00" else "$workStr",
                modifier = Modifier.weight(1.4f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isOff) OffDaySlate else WorkedGreen
            )

            // Overtime
            Text(
                text = if (hasOt) "+$otStr" else "0:00",
                modifier = Modifier.weight(1.2f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (hasOt) OvertimeAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun EmptyRecordsCard(
    onSelectDay: () -> Unit,
    onImportClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "No Overtime Entries Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Pick a day (1-31) above to enter your Shift In, Shift Out, and Break times, or import an existing CSV file.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSelectDay) {
                    Text("Start with Day 1")
                }
                OutlinedButton(onClick = onImportClick) {
                    Text("Import CSV")
                }
            }
        }
    }
}
