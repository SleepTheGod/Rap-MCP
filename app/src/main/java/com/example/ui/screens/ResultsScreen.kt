package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Competitor
import com.example.repository.BattleUiState
import com.example.ui.theme.*

@Composable
fun ResultsScreen(
    uiState: BattleUiState,
    onRematch: () -> Unit,
    onNewBattle: () -> Unit,
    onOpenMcp: () -> Unit,
    onExportZip: () -> Unit
) {
    var showFullTranscript by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Winner Banner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        2.dp,
                        when (uiState.winner) {
                            Competitor.User -> GoldCrown
                            Competitor.AI -> ElectricCyan
                            else -> TextSecondary
                        },
                        RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = ArenaSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = when (uiState.winner) {
                            Competitor.User -> GoldCrown
                            Competitor.AI -> ElectricCyan
                            else -> TextSecondary
                        },
                        modifier = Modifier.size(54.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = when (uiState.winner) {
                            Competitor.User -> "VICTORY! YOU WIN THE CYPHER!"
                            Competitor.AI -> "CYBER-MC TAKES THE CROWN!"
                            else -> "BATTLE ENDED IN A DRAW!"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = when (uiState.winner) {
                            Competitor.User -> GoldCrown
                            Competitor.AI -> ElectricCyan
                            else -> TextPrimary
                        },
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Final Weighted Score: ${uiState.finalUserScore} (You) vs ${uiState.finalAiScore} (Cyber-MC)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.config.mode.title} • ${uiState.config.roundCount} Rounds (Final Round 1.5x Multiplier)",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRematch,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BattleCrimson)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("REMATCH", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onNewBattle,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("NEW CYPHER", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Round by Round Recap
        item {
            Text(
                text = "ROUND BY ROUND RESULTS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricCyan,
                letterSpacing = 1.sp
            )
        }

        items(uiState.rounds) { round ->
            val isFinal = (round.roundNumber == uiState.config.roundCount)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ArenaCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isFinal) GoldCrown.copy(alpha = 0.6f) else ArenaCardBorder
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Round ${round.roundNumber}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            if (isFinal) {
                                Spacer(modifier = Modifier.width(6.dp))
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text("1.5x Final", fontSize = 10.sp, color = GoldCrown) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = ArenaSurface)
                                )
                            }
                        }
                        Text(
                            text = when (round.winner) {
                                Competitor.User -> "Winner: User"
                                Competitor.AI -> "Winner: Cyber-MC"
                                else -> "Winner: Draw"
                            },
                            fontSize = 12.sp,
                            color = when (round.winner) {
                                Competitor.User -> BattleCrimson
                                Competitor.AI -> ElectricCyan
                                else -> TextSecondary
                            }
                        )
                    }

                    Text(
                        text = "${round.userScore} vs ${round.aiScore}",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }
            }
        }

        // Cumulative Statistics
        item {
            Text(
                text = "CUMULATIVE BATTLE STATISTICS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricCyan,
                letterSpacing = 1.sp
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ArenaCard)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatRow("Rounds Won", "${uiState.stats.userRoundsWon} (User) vs ${uiState.stats.aiRoundsWon} (AI)")
                    StatRow("Drawn Rounds", "${uiState.stats.drawnRounds}")
                    StatRow("Average Verse Score", "${uiState.stats.userAverageScore} (User) vs ${uiState.stats.aiAverageScore} (AI)")
                    StatRow("Highest Single Round Score", "${uiState.stats.userHighestScore} (User) vs ${uiState.stats.aiHighestScore} (AI)")
                    StatRow("Total Verses Delivered", "${uiState.stats.totalVerses}")
                    StatRow("Timeouts Count", "${uiState.stats.timeoutCount}")
                }
            }
        }

        // Full Transcript Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMPLETE BATTLE TRANSCRIPT",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan,
                    letterSpacing = 1.sp
                )

                TextButton(onClick = { showFullTranscript = !showFullTranscript }) {
                    Text(if (showFullTranscript) "Hide" else "Show All", color = BattleCrimson)
                }
            }
        }

        if (showFullTranscript) {
            items(uiState.transcript) { entry ->
                val isUser = (entry.competitor == Competitor.User)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (isUser) ArenaSurface else ArenaCard)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${if (isUser) "YOU" else "CYBER-MC"} • Round ${entry.roundNumber}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isUser) BattleCrimson else ElectricCyan
                            )
                            Text(
                                text = "${entry.score} pts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = entry.content,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Developer Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenMcp,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("MCP Inspector", fontSize = 11.sp)
                }

                Button(
                    onClick = onExportZip,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ArenaBlack)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export ZIP", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
