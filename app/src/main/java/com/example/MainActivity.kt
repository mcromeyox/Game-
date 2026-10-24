package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.GameMode
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PubgDarkBg
import com.example.viewmodel.GameViewModel

enum class Screen {
    LOBBY,
    GARAGE,
    ARMORY,
    CAREER,
    SETTINGS,
    GAME
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val gameViewModel: GameViewModel = viewModel()
                val uiState by gameViewModel.uiState.collectAsState()

                var currentScreen by remember { mutableStateOf(Screen.LOBBY) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PubgDarkBg
                ) {
                    when (currentScreen) {
                        Screen.LOBBY -> {
                            LobbyScreen(
                                viewModel = gameViewModel,
                                onStartGame = { mode ->
                                    gameViewModel.startMatch(mode)
                                    currentScreen = Screen.GAME
                                },
                                onNavigateToGarage = { currentScreen = Screen.GARAGE },
                                onNavigateToArmory = { currentScreen = Screen.ARMORY },
                                onNavigateToCareer = { currentScreen = Screen.CAREER },
                                onNavigateToSettings = { currentScreen = Screen.SETTINGS }
                            )
                        }

                        Screen.GAME -> {
                            GameScreen(
                                viewModel = gameViewModel,
                                onBackToLobby = {
                                    gameViewModel.exitMatch()
                                    currentScreen = Screen.LOBBY
                                }
                            )
                        }

                        Screen.GARAGE -> {
                            GarageScreen(
                                viewModel = gameViewModel,
                                onBack = { currentScreen = Screen.LOBBY },
                                onTestDrive = {
                                    gameViewModel.startMatch(GameMode.VEHICLE_WARFARE)
                                    currentScreen = Screen.GAME
                                }
                            )
                        }

                        Screen.ARMORY -> {
                            ArmoryScreen(
                                viewModel = gameViewModel,
                                onBack = { currentScreen = Screen.LOBBY }
                            )
                        }

                        Screen.CAREER -> {
                            CareerScreen(
                                viewModel = gameViewModel,
                                onBack = { currentScreen = Screen.LOBBY }
                            )
                        }

                        Screen.SETTINGS -> {
                            SettingsScreen(
                                viewModel = gameViewModel,
                                onBack = { currentScreen = Screen.LOBBY }
                            )
                        }
                    }
                }
            }
        }
    }
}
