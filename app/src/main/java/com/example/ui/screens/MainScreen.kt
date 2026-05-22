package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.TimeRecord
import com.example.R
import com.example.ui.components.CelebrationLayer
import com.example.ui.components.BackgroundWrapper
import com.example.ui.components.CustomTimePickerDialog
import com.example.ui.theme.AppBackgroundTheme
import com.example.viewmodel.TimeKeeperViewModel
import com.example.util.TimeUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: TimeKeeperViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State binders from ViewModel
    val activeTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedRecord by viewModel.selectedRecord.collectAsStateWithLifecycle()
    val todayRecord by viewModel.todayRecord.collectAsStateWithLifecycle()
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.liveElapsedSeconds.collectAsStateWithLifecycle()
    val selectedCountryCode by viewModel.selectedCountryCode.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val currentTimezoneId = remember(selectedCountryCode) {
        com.example.data.CountrySetting.getByCode(selectedCountryCode).timezoneId
    }

    // Dialog & UI temporary states
    var showThemeDialog by remember { mutableStateOf(false) }
    var settingsSection by remember { mutableStateOf("MAIN") }
    var showInTimeDialog by remember { mutableStateOf(false) }
    var showOutTimeDialog by remember { mutableStateOf(false) }
    var showReqHoursManualDialog by remember { mutableStateOf(false) }
    var showActDurationManualDialog by remember { mutableStateOf(false) }
    var isEditingTodayReqSettings by remember { mutableStateOf(false) }

    // Backup Editor States for the selected date manual editing
    var manualInTime by remember { mutableStateOf<String?>(null) }
    var manualOutTime by remember { mutableStateOf<String?>(null) }
    var manualRequiredHours by remember { mutableStateOf(8.0) }
    var manualActualMinutes by remember { mutableStateOf(0) }
    var manualNotes by remember { mutableStateOf("") }

    // When the selected date or selected physical record changes, sync editor states
    LaunchedEffect(selectedDate, selectedRecord) {
        val r = selectedRecord
        manualInTime = r?.checkInTime
        manualOutTime = r?.checkOutTime
        manualRequiredHours = r?.requiredHours ?: 8.0
        manualActualMinutes = r?.actualMinutesWorked ?: 0
        manualNotes = r?.notes ?: ""
    }

    // Auto calculate actual minutes based on manually selected "In" and "Out" times if available
    LaunchedEffect(manualInTime, manualOutTime) {
        val `in` = manualInTime
        val out = manualOutTime
        if (`in` != null && out != null) {
            val inMins = TimeUtils.parseHHmmToMinutes(`in`)
            val outMins = TimeUtils.parseHHmmToMinutes(out)
            if (inMins != null && outMins != null) {
                val diff = outMins - inMins
                // Handle split overnight shift wrapping cleanly
                manualActualMinutes = if (diff >= 0) diff else (diff + 1440)
            }
        }
    }

    val showCelebration = remember(todayRecord, elapsedSeconds) {
        val record = todayRecord
        if (record != null) {
            val reqMins = record.requiredHours * 60.0
            if (record.isTracking) {
                (elapsedSeconds / 60.0) >= reqMins
            } else {
                record.actualMinutesWorked >= reqMins && record.checkOutTime != null
            }
        } else {
            false
        }
    }

    BackgroundWrapper(theme = activeTheme) {
        CelebrationLayer(
            active = showCelebration,
            primaryColor = activeTheme.primaryColor,
            accentColor = activeTheme.accentColor
        )
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name).uppercase(java.util.Locale.getDefault()),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                settingsSection = "MAIN"
                                showThemeDialog = true
                            },
                            modifier = Modifier.testTag("theme_selector_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.title_settings),
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // SECTION 1: Current Ticking Dynamic Clock Header Card
                LiveClockCard(activeTheme, currentTimezoneId)

                // SECTION 2: Live Tracking Stopwatch Tracker (Đếm giờ chấm công)
                LiveStopwatchTrackerCard(
                    todayRecord = todayRecord,
                    elapsedSeconds = elapsedSeconds,
                    onCheckIn = { hours -> viewModel.checkIn(hours) },
                    onCheckOut = { viewModel.checkOut() },
                    onDeleteToday = { viewModel.deleteRecordOfSelected() },
                    activeTheme = activeTheme
                )

                // SECTION 3: Horizontal Calendar Ribbon
                CalendarRibbon(
                    selectedDate = selectedDate,
                    allRecords = allRecords,
                    onDateSelected = { dateStr -> viewModel.selectDate(dateStr) },
                    activeTheme = activeTheme,
                    currentTimezoneId = currentTimezoneId
                )

                // SECTION 4: Historic & Custom Attendance Details Editor (Quản lý lịch & giờ giấc)
                AttendanceDetailsEditorCard(
                    dateString = selectedDate,
                    inTime = manualInTime,
                    outTime = manualOutTime,
                    requiredHours = manualRequiredHours,
                    actualMinutes = manualActualMinutes,
                    notes = manualNotes,
                    onInTimeClick = { showInTimeDialog = true },
                    onOutTimeClick = { showOutTimeDialog = true },
                    onRequiredHoursChange = { manualRequiredHours = it },
                    onActualMinutesChange = { manualActualMinutes = it },
                    onNotesChange = { manualNotes = it },
                    onSave = {
                        viewModel.saveOrUpdateRecord(
                            dateString = selectedDate,
                            checkIn = manualInTime,
                            checkOut = manualOutTime,
                            requiredHours = manualRequiredHours,
                            actualMinutes = manualActualMinutes,
                            notes = manualNotes
                        )
                        val saveMsg = context.getString(R.string.toast_saved) + TimeUtils.formatShortMonthDay(selectedDate, currentTimezoneId)
                        Toast.makeText(context, saveMsg, Toast.LENGTH_SHORT).show()
                    },
                    onDelete = {
                        viewModel.deleteRecordOfSelected()
                        val delMsg = context.getString(R.string.toast_deleted) + TimeUtils.formatShortMonthDay(selectedDate, currentTimezoneId)
                        Toast.makeText(context, delMsg, Toast.LENGTH_SHORT).show()
                    },
                    onPresetRequiredHoursClick = { manualRequiredHours = it },
                    activeTheme = activeTheme,
                    currentTimezoneId = currentTimezoneId
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Time Pickers Dialogs integration
    if (showInTimeDialog) {
        CustomTimePickerDialog(
            initialTime = manualInTime ?: "08:00",
            onDismiss = { showInTimeDialog = false },
            onTimeSelected = {
                manualInTime = it
                showInTimeDialog = false
            }
        )
    }

    if (showOutTimeDialog) {
        CustomTimePickerDialog(
            initialTime = manualOutTime ?: "17:00",
            onDismiss = { showOutTimeDialog = false },
            onTimeSelected = {
                manualOutTime = it
                showOutTimeDialog = false
            }
        )
    }

    // Background Themes, Language, & Country Timezone dialog (Settings Panel)
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (settingsSection != "MAIN") {
                        IconButton(
                            onClick = { settingsSection = "MAIN" },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Text(
                        text = when (settingsSection) {
                            "REGION" -> stringResource(R.string.settings_region_title)
                            "LANGUAGE" -> stringResource(R.string.settings_language_title)
                            "THEME" -> stringResource(R.string.settings_background_title)
                            else -> stringResource(R.string.title_settings)
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (settingsSection) {
                        "MAIN" -> {
                            val countryName = stringResource(
                                com.example.data.CountrySetting.getByCode(selectedCountryCode).stringResId
                            )
                            SettingMenuItem(
                                icon = Icons.Default.LocationOn,
                                title = stringResource(R.string.settings_region_title),
                                subtitle = countryName,
                                onClick = { settingsSection = "REGION" },
                                activeTheme = activeTheme
                            )

                            val langLabel = if (selectedLanguage == "en") "English" else "Tiếng Việt"
                            SettingMenuItem(
                                icon = Icons.Default.Info,
                                title = stringResource(R.string.settings_language_title),
                                subtitle = langLabel,
                                onClick = { settingsSection = "LANGUAGE" },
                                activeTheme = activeTheme
                            )

                            SettingMenuItem(
                                icon = Icons.Default.Settings,
                                title = stringResource(R.string.settings_background_title),
                                subtitle = activeTheme.displayName,
                                onClick = { settingsSection = "THEME" },
                                activeTheme = activeTheme
                            )
                        }
                        "REGION" -> {
                            Text(
                                text = stringResource(R.string.settings_region_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            com.example.data.CountrySetting.ALL_COUNTRIES.forEach { country ->
                                val isCountrySelected = selectedCountryCode == country.code
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isCountrySelected) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.05f))
                                        .clickable {
                                            viewModel.selectCountry(country.code)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(id = country.stringResId),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isCountrySelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isCountrySelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isCountrySelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        "LANGUAGE" -> {
                            Text(
                                text = stringResource(R.string.label_select_language),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            listOf("vi" to R.string.language_vi, "en" to R.string.language_en).forEach { (langCode, stringRes) ->
                                val isLangSelected = selectedLanguage == langCode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isLangSelected) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.05f))
                                        .clickable {
                                            viewModel.selectLanguage(langCode)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(id = stringRes),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isLangSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isLangSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isLangSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        "THEME" -> {
                            Text(
                                text = stringResource(R.string.label_select_theme),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            AppBackgroundTheme.values().forEach { themeItem ->
                                val isSelected = activeTheme == themeItem
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.05f))
                                        .clickable {
                                            viewModel.selectTheme(themeItem)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Mini Color previews
                                    Row(
                                        modifier = Modifier.padding(end = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(themeItem.primaryColor)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(themeItem.accentColor)
                                        )
                                    }

                                    Text(
                                        text = themeItem.displayName,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        )
    }
}

// Sub-Component: Elegant hierarchical menu options
@Composable
fun SettingMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    activeTheme: AppBackgroundTheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp)
            )
            .background(Color.White.copy(alpha = 0.04f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(activeTheme.primaryColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = activeTheme.primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Navigate Next",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// Sub-Component: Dynamic local clock display
@Composable
fun LiveClockCard(activeTheme: AppBackgroundTheme, timezoneId: String) {
    var currentTimeStr by remember { mutableStateOf("") }
    var currentDateStr by remember { mutableStateOf("") }

    LaunchedEffect(timezoneId) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        timeFormat.timeZone = TimeZone.getTimeZone(timezoneId)
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("vi", "VN"))
        dateFormat.timeZone = TimeZone.getTimeZone(timezoneId)
        while (true) {
            val now = Date()
            currentTimeStr = timeFormat.format(now)
            currentDateStr = dateFormat.format(now)
            kotlinx.coroutines.delay(1000)
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTimeStr.ifEmpty { "--:--:--" },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp
                ),
                color = activeTheme.primaryColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = currentDateStr.ifEmpty { "Đang cập nhật..." },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Sub-Component: Live tracking stopwatch manager card
@Composable
fun LiveStopwatchTrackerCard(
    todayRecord: TimeRecord?,
    elapsedSeconds: Long,
    onCheckIn: (Double) -> Unit,
    onCheckOut: () -> Unit,
    onDeleteToday: () -> Unit,
    activeTheme: AppBackgroundTheme
) {
    val isTracking = todayRecord?.isTracking == true
    var requiredHoursInput by remember { mutableStateOf(8.0) }
    var showTodayReqHoursManualDialog by remember { mutableStateOf(false) }

    val outTimeCalculated = remember(todayRecord?.checkInTime, todayRecord?.requiredHours) {
        val checkIn = todayRecord?.checkInTime ?: ""
        val req = todayRecord?.requiredHours ?: 8.0
        if (checkIn.isNotEmpty()) {
            TimeUtils.calculateCheckOutTime(checkIn, req)
        } else {
            "--:--"
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = stringResource(R.string.today_status),
                        tint = activeTheme.primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.today_status),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }

                if (isTracking) {
                    Surface(
                        color = activeTheme.primaryColor.copy(alpha = 0.2f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, activeTheme.primaryColor.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(activeTheme.accentColor)
                            )
                            Text(
                                text = stringResource(R.string.status_running),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeTheme.accentColor
                            )
                        }
                    }
                } else {
                    Text(
                        text = todayRecord?.checkOutTime?.let { stringResource(R.string.status_completed) } ?: stringResource(R.string.status_not_started),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (todayRecord?.checkOutTime != null) activeTheme.accentColor else Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Divider(color = Color.White.copy(alpha = 0.1f))

            if (!isTracking) {
                // Not Clocked In or Completed
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (todayRecord?.checkOutTime != null) {
                        // Show checkout results
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            tint = activeTheme.accentColor,
                            modifier = Modifier.size(48.dp)
                        )

                        Text(
                            text = stringResource(R.string.worked_from_to, todayRecord.checkInTime ?: "", todayRecord.checkOutTime ?: ""),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )

                        Text(
                            text = stringResource(R.string.worked_accumulated, todayRecord.actualMinutesWorked),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        OutlinedButton(
                            onClick = onDeleteToday,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Làm mới")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_delete_today))
                        }
                    } else {
                        // Pure Start state
                        Text(
                            text = stringResource(R.string.not_started_tip),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Input Hours section
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.target_hours_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .clickable { showTodayReqHoursManualDialog = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${requiredHoursInput} " + stringResource(R.string.hours_suffix),
                                        fontWeight = FontWeight.Bold,
                                        color = activeTheme.primaryColor,
                                        fontSize = 14.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Nhập tay",
                                        tint = activeTheme.primaryColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Slider(
                                value = requiredHoursInput.toFloat(),
                                onValueChange = { requiredHoursInput = Math.round(it * 2f).toDouble() / 2.0 },
                                valueRange = 1f..16f,
                                steps = 29, // steps every 0.5 hours
                                colors = SliderDefaults.colors(
                                    activeTrackColor = activeTheme.primaryColor,
                                    thumbColor = activeTheme.primaryColor
                                ),
                                modifier = Modifier.testTag("required_hours_slider")
                            )

                            // Preset chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                            ) {
                                listOf(4.0, 8.0, 9.0, 10.0).forEach { preset ->
                                    FilterChip(
                                        selected = requiredHoursInput == preset,
                                        onClick = { requiredHoursInput = preset },
                                        label = { Text("${preset}h") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = activeTheme.primaryColor,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { onCheckIn(requiredHoursInput) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("check_in_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeTheme.primaryColor,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.btn_start_shift),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        if (showTodayReqHoursManualDialog) {
                            CustomNumberInputDialog(
                                title = stringResource(R.string.input_target_hours_title),
                                initialValue = requiredHoursInput,
                                onDismiss = { showTodayReqHoursManualDialog = false },
                                onValueConfirmed = {
                                    requiredHoursInput = it
                                    showTodayReqHoursManualDialog = false
                                }
                            )
                        }
                    }
                }
            } else {
                // Active Tracking Block (Đang tính giờ)
                val hours = elapsedSeconds / 3600
                val minutes = (elapsedSeconds % 3600) / 60
                val seconds = elapsedSeconds % 60
                val formattedProgress = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                // Calculate worked progression
                val totalMinsWorked = (elapsedSeconds / 60.0)
                val requiredMins = (todayRecord.requiredHours * 60.0)
                val percentProgress = if (requiredMins > 0) (totalMinsWorked / requiredMins).toFloat().coerceIn(0f, 1f) else 0f

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.live_progress_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = formattedProgress,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        modifier = Modifier.testTag("stopwatch_text")
                    )

                    // Linear elegant progress bar
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = percentProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = activeTheme.accentColor,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.indicator_progress, (percentProgress * 100).toInt()),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = stringResource(R.string.indicator_target, todayRecord.requiredHours.toString()),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Active Overtime (OT) Display
                    val reqSeconds = (todayRecord.requiredHours * 3600).toLong()
                    if (elapsedSeconds > reqSeconds) {
                        val otSeconds = (elapsedSeconds - reqSeconds).coerceAtLeast(0L)
                        val otMins = (otSeconds / 60).toInt()
                        val otDisplay = if (otMins >= 60) {
                            stringResource(R.string.ot_value_hours, otMins / 60.0)
                        } else {
                            stringResource(R.string.ot_value_minutes, otMins)
                        }

                        Surface(
                            color = activeTheme.accentColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, activeTheme.accentColor.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "OT Badge",
                                        tint = activeTheme.accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.ot_congrats),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeTheme.accentColor
                                    )
                                }
                                Text(
                                    text = "+$otDisplay",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = activeTheme.accentColor
                                )
                            }
                        }
                    }

                    // Times summary row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.in_time_label),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = todayRecord.checkInTime ?: "--:--",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier.height(30.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.expected_out_label),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = outTimeCalculated,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeTheme.primaryColor
                            )
                        }
                    }

                    Button(
                        onClick = onCheckOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("check_out_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeTheme.accentColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Check Out")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_end_shift),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// Sub-Component: Dynamic scrollable horizontal Calendar Ribbon
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarRibbon(
    selectedDate: String,
    allRecords: List<TimeRecord>,
    onDateSelected: (String) -> Unit,
    activeTheme: AppBackgroundTheme,
    currentTimezoneId: String
) {
    val listState = rememberLazyListState()
    val rawDays = remember(currentTimezoneId) { TimeUtils.getCalendarDaysList(currentTimezoneId) }
    val todayStr = remember(currentTimezoneId) { TimeUtils.getCurrentDateString(currentTimezoneId) }

    // Scroll to today's date upon launch and today selection changes
    LaunchedEffect(todayStr) {
        val todayIndex = rawDays.indexOf(todayStr)
        if (todayIndex >= 0) {
            // Scroll to center today
            val scrollTo = (todayIndex - 3).coerceAtLeast(0)
            listState.scrollToItem(scrollTo)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.date_attendance_title),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.padding(start = 4.dp)
        )

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            items(rawDays) { day ->
                val record = allRecords.firstOrNull { it.dateString == day }
                val isSelected = selectedDate == day
                val isToday = day == todayStr

                // Color coding indicators
                val statusBorderColor = when {
                    isSelected -> activeTheme.primaryColor
                    isToday -> Color.White.copy(alpha = 0.4f)
                    else -> Color.White.copy(alpha = 0.1f)
                }

                val indicatorColor = when {
                    record == null -> Color.Transparent
                    record.isTracking -> activeTheme.primaryColor
                    record.checkOutTime != null && record.actualMinutesWorked >= (record.requiredHours * 60) -> activeTheme.accentColor
                    else -> Color.Yellow.copy(alpha = 0.8f)
                }

                Box(
                    modifier = Modifier
                        .width(62.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = statusBorderColor,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onDateSelected(day) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Short date string
                        val dParts = day.split("-")
                        val dayNum = dParts.getOrNull(2) ?: ""
                        val monthNum = dParts.getOrNull(1) ?: ""

                        Text(
                            text = if (isToday) stringResource(R.string.today_label) else "$dayNum/$monthNum",
                            fontSize = 12.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) activeTheme.primaryColor else Color.White
                        )

                        Text(
                            text = dayNum,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // Progress indicator dot at the bottom
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (indicatorColor == Color.Transparent) Color.White.copy(alpha = 0.1f) else indicatorColor)
                        )
                    }
                }
            }
        }
    }
}

// Sub-Component: Interactive detailed manual attendance editor (Quản lý giờ giấc với lịch)
@Composable
fun AttendanceDetailsEditorCard(
    dateString: String,
    inTime: String?,
    outTime: String?,
    requiredHours: Double,
    actualMinutes: Int,
    notes: String,
    onInTimeClick: () -> Unit,
    onOutTimeClick: () -> Unit,
    onRequiredHoursChange: (Double) -> Unit,
    onActualMinutesChange: (Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onPresetRequiredHoursClick: (Double) -> Unit,
    activeTheme: AppBackgroundTheme,
    currentTimezoneId: String
) {
    val dateLabel = remember(dateString, currentTimezoneId) { TimeUtils.getFriendlyDate(dateString, currentTimezoneId) }
    var currentActualHoursDecimal by remember(actualMinutes) {
        mutableStateOf(actualMinutes / 60.0)
    }
    var showReqHoursManualDialog by remember { mutableStateOf(false) }
    var showActDurationManualDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.details_title),
                        tint = activeTheme.primaryColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.details_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_day_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xoá ca",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = activeTheme.primaryColor
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Check-in and Check-out selections
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Chek-in edit block
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { onInTimeClick() }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.detail_checkin_title),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = inTime ?: "--:--",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.detail_tap_to_change),
                        fontSize = 10.sp,
                        color = activeTheme.primaryColor
                    )
                }

                // Check-out edit block
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { onOutTimeClick() }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.detail_checkout_title),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = outTime ?: "--:--",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.detail_tap_to_change),
                        fontSize = 10.sp,
                        color = activeTheme.primaryColor
                    )
                }
            }

            // Target working hours slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.detail_hours_required),
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { showReqHoursManualDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${requiredHours} " + stringResource(R.string.hours_suffix),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Nhập tay",
                            tint = activeTheme.primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Slider(
                    value = requiredHours.toFloat(),
                    onValueChange = { onRequiredHoursChange(Math.round(it * 2f).toDouble() / 2.0) },
                    valueRange = 1f..16f,
                    steps = 29,
                    colors = SliderDefaults.colors(
                        activeTrackColor = activeTheme.primaryColor,
                        thumbColor = activeTheme.primaryColor
                    )
                )
            }

            // Actual hours worked manually
            Column(modifier = Modifier.fillMaxWidth()) {
                val actHours = actualMinutes / 60
                val actMins = actualMinutes % 60
                val displayActual = stringResource(R.string.actual_worked_format, actHours, actMins)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.detail_actual_worked),
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { showActDurationManualDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = displayActual,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.accentColor,
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Nhập tay",
                            tint = activeTheme.accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Slider(
                    value = currentActualHoursDecimal.toFloat(),
                    onValueChange = {
                        val roundedHours = Math.round(it * 12f) / 12f // 5-minute increments
                        currentActualHoursDecimal = roundedHours.toDouble()
                        onActualMinutesChange((roundedHours * 60).toInt())
                    },
                    valueRange = 0f..16f,
                    steps = 191, // 5 minute steps (16 * 12 - 1)
                    colors = SliderDefaults.colors(
                        activeTrackColor = activeTheme.accentColor,
                        thumbColor = activeTheme.accentColor
                    )
                )

                // Render Overtime helper banner for current editor selection
                val targetMinutes = (requiredHours * 60).toInt()
                if (actualMinutes > targetMinutes) {
                    val otMinutes = actualMinutes - targetMinutes
                    val otDisplay = if (otMinutes >= 60) {
                        stringResource(R.string.ot_value_hours, otMinutes / 60.0)
                    } else {
                        stringResource(R.string.ot_value_minutes, otMinutes)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(activeTheme.accentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.ot_label),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.accentColor
                        )
                        Text(
                            text = "+$otDisplay",
                            fontWeight = FontWeight.ExtraBold,
                            color = activeTheme.accentColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (showReqHoursManualDialog) {
                CustomNumberInputDialog(
                    title = stringResource(R.string.input_required_hours_title),
                    initialValue = requiredHours,
                    onDismiss = { showReqHoursManualDialog = false },
                    onValueConfirmed = {
                        onRequiredHoursChange(it)
                        showReqHoursManualDialog = false
                    }
                )
            }

            if (showActDurationManualDialog) {
                CustomDurationInputDialog(
                    initialMinutes = actualMinutes,
                    onDismiss = { showActDurationManualDialog = false },
                    onDurationConfirmed = {
                        onActualMinutesChange(it)
                        showActDurationManualDialog = false
                    }
                )
            }

            // Notes input box
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(R.string.detail_notes_label), color = Color.White.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_input_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = activeTheme.primaryColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = activeTheme.primaryColor,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                ),
                maxLines = 3,
                singleLine = false
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_attendance_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeTheme.primaryColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Save Progress")
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.btn_detail_save_label),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun CustomNumberInputDialog(
    title: String,
    initialValue: Double,
    onDismiss: () -> Unit,
    onValueConfirmed: (Double) -> Unit
) {
    var textValue by remember { mutableStateOf(initialValue.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val invalidHoursError = stringResource(R.string.input_error_invalid_hours)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.input_hours_placeholder_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel_btn).uppercase())
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            val doubleVal = textValue.toDoubleOrNull()
                            if (doubleVal == null || doubleVal <= 0 || doubleVal > 24) {
                                errorMessage = invalidHoursError
                            } else {
                                onValueConfirmed(doubleVal)
                            }
                        }
                    ) {
                        Text(stringResource(R.string.confirm_btn).uppercase())
                    }
                }
            }
        }
    }
}

@Composable
fun CustomDurationInputDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onDurationConfirmed: (Int) -> Unit
) {
    val initialHours = initialMinutes / 60
    val initialRemainingMins = initialMinutes % 60

    var hoursStr by remember { mutableStateOf(initialHours.toString()) }
    var minsStr by remember { mutableStateOf(initialRemainingMins.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val errorHours = stringResource(R.string.input_error_hours_limit)
    val errorMins = stringResource(R.string.input_error_minutes_limit)
    val errorZero = stringResource(R.string.input_error_time_zero)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.input_duration_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hoursStr,
                        onValueChange = { if (it.all { c -> c.isDigit() }) hoursStr = it },
                        label = { Text(stringResource(R.string.input_hours_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = minsStr,
                        onValueChange = { if (it.all { c -> c.isDigit() }) minsStr = it },
                        label = { Text(stringResource(R.string.input_minutes_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel_btn).uppercase())
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            val hrs = hoursStr.toIntOrNull() ?: 0
                            val mins = minsStr.toIntOrNull() ?: 0
                            if (hrs < 0 || hrs > 24) {
                                errorMessage = errorHours
                                return@Button
                            }
                            if (mins < 0 || mins > 59) {
                                errorMessage = errorMins
                                return@Button
                            }
                            val totalMins = hrs * 60 + mins
                            if (totalMins <= 0 && hrs == 0 && mins == 0) {
                                errorMessage = errorZero
                                return@Button
                            }
                            onDurationConfirmed(totalMins)
                        }
                    ) {
                        Text(stringResource(R.string.confirm_btn).uppercase())
                    }
                }
            }
        }
    }
}

