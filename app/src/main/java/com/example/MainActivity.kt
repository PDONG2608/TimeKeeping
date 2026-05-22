package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.example.data.db.AppDatabase
import com.example.data.repository.TimeRecordRepositoryImpl
import com.example.domain.usecase.*
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemePreferences
import com.example.viewmodel.TimeKeeperViewModel
import com.example.viewmodel.TimeKeeperViewModelFactory

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
