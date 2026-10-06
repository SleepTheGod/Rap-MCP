package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.BattleEntity
import com.example.model.Round
import com.example.repository.BattleRepository
import com.example.ui.screens.*
import com.example.ui.theme.ArenaBlack
import com.example.ui.theme.BattleCrimson
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.RapBattleTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

enum class AppScreen {
    LOBBY,
    ARENA,
    RESULTS,
    MCP_STUDIO,
    HISTORY,
    PRODUCTION_EXPORT
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RapBattleTheme {
                val context = LocalContext.current
                val repository = remember { BattleRepository(context) }
                val uiState by repository.uiState.collectAsStateWithLifecycle()
                val battleHistory by repository.getBattleHistory().collectAsStateWithLifecycle(initialValue = emptyList())

                var currentScreen by remember { mutableStateOf(AppScreen.LOBBY) }
                var inspectingRound by remember { mutableStateOf<Round?>(null) }

                // Top-level Scaffold with proper WindowInsets and Edge-to-Edge handling
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = ArenaBlack,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        if (currentScreen in listOf(AppScreen.LOBBY, AppScreen.HISTORY, AppScreen.MCP_STUDIO, AppScreen.PRODUCTION_EXPORT)) {
                            NavigationBar(
                                containerColor = Color(0xFF101018),
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.LOBBY,
                                    onClick = { currentScreen = AppScreen.LOBBY },
                                    icon = { Icon(Icons.Default.Mic, contentDescription = "Cypher Lobby") },
                                    label = { Text("Lobby", fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = BattleCrimson,
                                        selectedTextColor = BattleCrimson,
                                        indicatorColor = Color(0xFF201525),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.HISTORY,
                                    onClick = { currentScreen = AppScreen.HISTORY },
                                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                    label = { Text("History", fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ElectricCyan,
                                        selectedTextColor = ElectricCyan,
                                        indicatorColor = Color(0xFF102530),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.MCP_STUDIO,
                                    onClick = { currentScreen = AppScreen.MCP_STUDIO },
                                    icon = { Icon(Icons.Default.Bolt, contentDescription = "MCP Protocol") },
                                    label = { Text("MCP Tools", fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ElectricCyan,
                                        selectedTextColor = ElectricCyan,
                                        indicatorColor = Color(0xFF102530),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.PRODUCTION_EXPORT,
                                    onClick = { currentScreen = AppScreen.PRODUCTION_EXPORT },
                                    icon = { Icon(Icons.Default.Archive, contentDescription = "Production ZIP") },
                                    label = { Text("Ship ZIP", fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = BattleCrimson,
                                        selectedTextColor = BattleCrimson,
                                        indicatorColor = Color(0xFF251520),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            AppScreen.LOBBY -> {
                                LobbyScreen(
                                    currentConfig = uiState.config,
                                    onStartBattle = { newConfig ->
                                        repository.initializeNewBattle(newConfig)
                                        currentScreen = AppScreen.ARENA
                                    },
                                    onNavigateToMcp = { currentScreen = AppScreen.MCP_STUDIO },
                                    onNavigateToExport = { currentScreen = AppScreen.PRODUCTION_EXPORT }
                                )
                            }

                            AppScreen.ARENA -> {
                                BackHandler {
                                    currentScreen = AppScreen.LOBBY
                                }
                                ArenaScreen(
                                    uiState = uiState,
                                    onSubmitVerse = { verseText ->
                                        repository.submitUserVerse(verseText)
                                    },
                                    onProceedNextRound = {
                                        repository.proceedToNextRound()
                                    },
                                    onViewScorecard = { round ->
                                        inspectingRound = round
                                    },
                                    onViewFinalResults = {
                                        currentScreen = AppScreen.RESULTS
                                    },
                                    onQuitBattle = {
                                        currentScreen = AppScreen.LOBBY
                                    }
                                )
                            }

                            AppScreen.RESULTS -> {
                                BackHandler {
                                    currentScreen = AppScreen.LOBBY
                                }
                                ResultsScreen(
                                    uiState = uiState,
                                    onRematch = {
                                        repository.createRematch()
                                        currentScreen = AppScreen.ARENA
                                    },
                                    onNewBattle = {
                                        currentScreen = AppScreen.LOBBY
                                    },
                                    onOpenMcp = {
                                        currentScreen = AppScreen.MCP_STUDIO
                                    },
                                    onExportZip = {
                                        currentScreen = AppScreen.PRODUCTION_EXPORT
                                    }
                                )
                            }

                            AppScreen.MCP_STUDIO -> {
                                BackHandler {
                                    currentScreen = AppScreen.LOBBY
                                }
                                McpStudioScreen(
                                    currentBattleState = repository.getMcpStateJson(),
                                    onBack = { currentScreen = AppScreen.LOBBY }
                                )
                            }

                            AppScreen.HISTORY -> {
                                BackHandler {
                                    currentScreen = AppScreen.LOBBY
                                }
                                HistoryScreen(
                                    battles = battleHistory,
                                    onBack = { currentScreen = AppScreen.LOBBY }
                                )
                            }

                            AppScreen.PRODUCTION_EXPORT -> {
                                BackHandler {
                                    currentScreen = AppScreen.LOBBY
                                }
                                ProductionExportScreen(
                                    onBack = { currentScreen = AppScreen.LOBBY }
                                )
                            }
                        }

                        // Scorecard Modal Sheet if active
                        inspectingRound?.let { round ->
                            ScorecardDialog(
                                round = round,
                                onDismiss = { inspectingRound = null }
                            )
                        }
                    }
                }
            }
        }
    }
}
