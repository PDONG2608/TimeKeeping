package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
