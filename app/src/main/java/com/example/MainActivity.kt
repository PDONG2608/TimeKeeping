package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.AppDatabase
import com.example.data.TimeRecordRepository
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemePreferences
import com.example.viewmodel.TimeKeeperViewModel
import com.example.viewmodel.TimeKeeperViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup database, repository and local settings preferences
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TimeRecordRepository(database.timeRecordDao())
        val themePreferences = ThemePreferences(applicationContext)
        
        // Instantiate ViewModel
        val viewModel: TimeKeeperViewModel by viewModels {
            TimeKeeperViewModelFactory(repository, themePreferences)
        }
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
