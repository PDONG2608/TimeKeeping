package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import com.example.data.db.AppDatabase
import com.example.data.repository.TimeRecordRepositoryImpl
import com.example.domain.usecase.*
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemePreferences
import com.example.viewmodel.TimeKeeperViewModel
import com.example.viewmodel.TimeKeeperViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Assemble Data Layer
        val database = AppDatabase.getDatabase(applicationContext)
        val timeRecordDao = database.timeRecordDao()
        val repository = TimeRecordRepositoryImpl(timeRecordDao)
        
        // Assemble Domain Layer Use Cases
        val getAllRecordsUseCase = GetAllRecordsUseCase(repository)
        val getRecordByDateUseCase = GetRecordByDateUseCase(repository)
        val getRecordByDateDirectUseCase = GetRecordByDateDirectUseCase(repository)
        val saveRecordUseCase = SaveRecordUseCase(repository)
        val deleteRecordUseCase = DeleteRecordUseCase(repository)
        
        val useCases = TimeRecordUseCases(
            getAllRecords = getAllRecordsUseCase,
            getRecordByDate = getRecordByDateUseCase,
            getRecordByDateDirect = getRecordByDateDirectUseCase,
            saveRecord = saveRecordUseCase,
            deleteRecord = deleteRecordUseCase
        )
        
        // Assemble Presentation State & Preferences
        val themePreferences = ThemePreferences(applicationContext)
        
        // Instantiate ViewModel using SOLID-assembled components
        val viewModel: TimeKeeperViewModel by viewModels {
            TimeKeeperViewModelFactory(useCases, themePreferences)
        }
        
        // Setup Notifications
        createNotificationChannel()
        requestNotificationPermission()

        // Listen for shift completion event
        lifecycleScope.launch {
            viewModel.targetReachedEvent.collect {
                showShiftCompleteNotification(viewModel.selectedLanguage.value)
            }
        }

        enableEdgeToEdge()
        setContent {
            val selectedLanguageState = viewModel.selectedLanguage.collectAsState()
            MyApplicationTheme {
                LocalizedApp(languageCode = selectedLanguageState.value) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel("SHIFT_COMPLETE_CHANNEL", name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                enableVibration(true)
            }
            val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun showShiftCompleteNotification(langCode: String) {
        val locale = java.util.Locale(langCode)
        val config = android.content.res.Configuration(resources.configuration)
        config.setLocale(locale)
        val localizedContext = createConfigurationContext(config)

        val title = localizedContext.getString(R.string.notification_title)
        val content = localizedContext.getString(R.string.notification_content)

        val intent = android.content.Intent(this, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = androidx.core.app.NotificationCompat.Builder(this, "SHIFT_COMPLETE_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)

        val notificationManager = androidx.core.app.NotificationManagerCompat.from(this)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    notificationManager.notify(1001, builder.build())
                } catch (_: SecurityException) {}
            }
        } else {
            try {
                notificationManager.notify(1001, builder.build())
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun LocalizedApp(languageCode: String, content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val localizedContext = androidx.compose.runtime.remember(languageCode, context) {
        val locale = java.util.Locale(languageCode)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalContext provides localizedContext
    ) {
        content()
    }
}
