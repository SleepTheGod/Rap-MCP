package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.BattleEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    battles: List<BattleEntity>,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = ArenaBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "BATTLE CYPHER HISTORY",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ArenaSurface)
            )
        }
    ) { innerPadding ->
        if (battles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No saved battles yet",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Complete your first battle in the Arena to record stats.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(battles) { battle ->
                    val isUserWinner = (battle.winner == "User")
                    val isDraw = (battle.winner == "Draw" || battle.winner == null)
                    val dateFormatted = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                        .format(Date(battle.createdAt))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ArenaCard),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isUserWinner) GoldCrown.copy(alpha = 0.5f) else ArenaCardBorder
                            )
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = if (isUserWinner) GoldCrown else if (isDraw) TextSecondary else ElectricCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when {
                                            isUserWinner -> "USER VICTORY"
                                            isDraw -> "DRAW"
                                            else -> "CYBER-MC WON"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isUserWinner) GoldCrown else if (isDraw) TextSecondary else ElectricCyan
                                    )
                                }

                                Text(
                                    text = "${battle.finalUserScore} vs ${battle.finalAiScore}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${battle.mode} • ${battle.roundCount} Rounds • ${battle.difficulty}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = dateFormatted,
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }

                            if (!battle.theme.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Theme: ${battle.theme}",
                                    fontSize = 11.sp,
                                    color = ElectricCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
