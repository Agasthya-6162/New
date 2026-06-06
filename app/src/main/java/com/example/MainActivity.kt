package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.data.CollegeDatabase
import com.example.data.CollegeRepository
import com.example.data.SessionManager
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize local DB client, DAOs, and abstract Repository
        val database = CollegeDatabase.getDatabase(this)
        val repository = CollegeRepository(database.collegeDao())

        // 2. Initialize SharedPreferences Session manager
        val sessionManager = SessionManager(this)

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        MainAppContent(repository, sessionManager)
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(repository: CollegeRepository, sessionManager: SessionManager) {
    // Top State Router
    val currentScreen = remember { mutableStateOf(AppScreen.Splash) }
    val currentUserId = remember { mutableStateOf(-1L) }
    var isDbSeeded = remember { mutableStateOf(false) }

    when (currentScreen.value) {
        AppScreen.Splash, AppScreen.Login, AppScreen.ApprovalPending -> {
            AuthMainContent(
                repository = repository,
                sessionManager = sessionManager,
                currentScreen = currentScreen,
                currentUserId = currentUserId,
                onInitComplete = { isDbSeeded.value = true }
            )
        }
        AppScreen.StudentDashboard -> {
            StudentDashboardScreen(
                repository = repository,
                sessionManager = sessionManager,
                currentScreen = currentScreen,
                userId = currentUserId.value
            )
        }
        AppScreen.StaffDashboard -> {
            StaffDashboardScreen(
                repository = repository,
                sessionManager = sessionManager,
                currentScreen = currentScreen,
                userId = currentUserId.value
            )
        }
        AppScreen.AdminDashboard -> {
            AdminDashboardScreen(
                repository = repository,
                sessionManager = sessionManager,
                currentScreen = currentScreen,
                userId = currentUserId.value
            )
        }
    }
}
