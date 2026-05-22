package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.TimeRecord
import com.example.data.TimeRecordRepository
import com.example.data.CountrySetting
import com.example.ui.theme.AppBackgroundTheme
import com.example.ui.theme.ThemePreferences
import com.example.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TimeKeeperViewModel(
    private val repository: TimeRecordRepository,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    // Theme state
    private val _currentTheme = MutableStateFlow(themePreferences.getBackgroundTheme())
    val currentTheme: StateFlow<AppBackgroundTheme> = _currentTheme.asStateFlow()

    // Country/Timezone state
    private val _selectedCountryCode = MutableStateFlow(themePreferences.getSelectedCountryCode())
    val selectedCountryCode: StateFlow<String> = _selectedCountryCode.asStateFlow()

    val currentTimezoneId: String
        get() = CountrySetting.getByCode(_selectedCountryCode.value).timezoneId

    // Date currently selected in the calendar
    private val _selectedDate = MutableStateFlow(
        TimeUtils.getCurrentDateString(
            CountrySetting.getByCode(themePreferences.getSelectedCountryCode()).timezoneId
        )
    )
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // List of all history records
    val allRecords: StateFlow<List<TimeRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Active record for the selected date
    val selectedRecord: StateFlow<TimeRecord?> = _selectedDate
        .flatMapLatest { date -> repository.getRecordByDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Current record for "today" to drive home tracking state, reacting to both timezone and repo updates
    val todayRecord: StateFlow<TimeRecord?> = _selectedCountryCode
        .flatMapLatest { code ->
            val tz = CountrySetting.getByCode(code).timezoneId
            repository.getRecordByDate(TimeUtils.getCurrentDateString(tz))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Live ticking states
    private val _liveElapsedSeconds = MutableStateFlow(0L)
    val liveElapsedSeconds: StateFlow<Long> = _liveElapsedSeconds.asStateFlow()

    private var tickerJob: Job? = null

    init {
        // Observe today's tracking status and start/stop the stopwatch ticker
        viewModelScope.launch {
            todayRecord.collect { record ->
                if (record != null && record.isTracking && record.checkInTimestamp != null) {
                    startTicker(record.checkInTimestamp)
                } else {
                    stopTicker()
                }
            }
        }
    }

    private fun startTicker(startTimestamp: Long) {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                val elapsedMs = System.currentTimeMillis() - startTimestamp
                _liveElapsedSeconds.value = (elapsedMs / 1000).coerceAtLeast(0)
                delay(1000)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
        _liveElapsedSeconds.value = 0
    }

    // Change background theme
    fun selectTheme(theme: AppBackgroundTheme) {
        themePreferences.setBackgroundTheme(theme)
        _currentTheme.value = theme
    }

    // Change selected country setting
    fun selectCountry(code: String) {
        themePreferences.setSelectedCountryCode(code)
        _selectedCountryCode.value = code
        // Update selected date to today in the chosen timezone
        val tz = CountrySetting.getByCode(code).timezoneId
        _selectedDate.value = TimeUtils.getCurrentDateString(tz)
    }

    // Select calendar date
    fun selectDate(dateString: String) {
        _selectedDate.value = dateString
    }

    // Move to next or previous date
    fun selectRelativeDate(days: Int) {
        try {
            val tz = currentTimezoneId
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            format.timeZone = java.util.TimeZone.getTimeZone(tz)
            val date = format.parse(_selectedDate.value) ?: java.util.Date()
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone(tz))
            cal.time = date
            cal.add(java.util.Calendar.DAY_OF_YEAR, days)
            _selectedDate.value = format.format(cal.time)
        } catch (_: Exception) {}
    }

    // Check IN (Vào ca)
    fun checkIn(requiredHours: Double = 8.0) {
        viewModelScope.launch {
            val tz = currentTimezoneId
            val todayStr = TimeUtils.getCurrentDateString(tz)
            val currentTime = TimeUtils.getCurrentTimeFormatted(tz)
            val existing = repository.getRecordByDateDirect(todayStr)

            val updated = TimeRecord(
                dateString = todayStr,
                checkInTime = currentTime,
                checkInTimestamp = System.currentTimeMillis(),
                checkOutTime = null,
                requiredHours = existing?.requiredHours ?: requiredHours,
                actualMinutesWorked = existing?.actualMinutesWorked ?: 0,
                isTracking = true,
                notes = existing?.notes ?: ""
            )
            repository.insertRecord(updated)
            // Select today automatically
            _selectedDate.value = todayStr
        }
    }

    // Check OUT (Ra ca)
    fun checkOut() {
        viewModelScope.launch {
            val tz = currentTimezoneId
            val todayStr = TimeUtils.getCurrentDateString(tz)
            val existing = repository.getRecordByDateDirect(todayStr) ?: return@launch
            if (!existing.isTracking || existing.checkInTimestamp == null) return@launch

            val checkoutTime = TimeUtils.getCurrentTimeFormatted(tz)
            val elapsedMinutes = ((System.currentTimeMillis() - existing.checkInTimestamp) / 60000).toInt().coerceAtLeast(1)

            val updated = existing.copy(
                checkOutTime = checkoutTime,
                isTracking = false,
                actualMinutesWorked = elapsedMinutes,
                checkInTimestamp = null
            )
            repository.insertRecord(updated)
        }
    }

    // Manual Save / Update record for any selected date
    fun saveOrUpdateRecord(
        dateString: String,
        checkIn: String?,
        checkOut: String?,
        requiredHours: Double,
        actualMinutes: Int,
        notes: String
    ) {
        viewModelScope.launch {
            val existing = repository.getRecordByDateDirect(dateString)
            val updated = TimeRecord(
                dateString = dateString,
                checkInTime = checkIn?.trim()?.ifEmpty { null },
                checkOutTime = checkOut?.trim()?.ifEmpty { null },
                checkInTimestamp = existing?.checkInTimestamp,
                requiredHours = requiredHours,
                actualMinutesWorked = actualMinutes,
                isTracking = existing?.isTracking ?: false,
                notes = notes
            )
            repository.insertRecord(updated)
        }
    }

    // Quick preset update of working hours for selected date
    fun updateRequiredHoursForSelected(hours: Double) {
        val date = _selectedDate.value
        viewModelScope.launch {
            val existing = repository.getRecordByDateDirect(date)
            val updated = existing?.copy(requiredHours = hours) ?: TimeRecord(
                dateString = date,
                requiredHours = hours
            )
            repository.insertRecord(updated)
        }
    }

    // Reset or Delete selected day record
    fun deleteRecordOfSelected() {
        val date = _selectedDate.value
        viewModelScope.launch {
            repository.deleteRecord(date)
        }
    }
}

class TimeKeeperViewModelFactory(
    private val repository: TimeRecordRepository,
    private val themePreferences: ThemePreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimeKeeperViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimeKeeperViewModel(repository, themePreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
