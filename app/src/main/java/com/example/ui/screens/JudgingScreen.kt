package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Round
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScorecardDialog(
    round: Round,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ArenaSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = ArenaCardBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Gavel, contentDescription = null, tint = GoldCrown)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ROUND ${round.roundNumber} SCORECARD",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Score Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ArenaCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ArenaCardBorder))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("YOU", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BattleCrimson)
                        Text(
                            text = "${round.userScore}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text("Normalized / 100", fontSize = 10.sp, color = TextMuted)
                    }

                    Text("VS", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextMuted)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CYBER-MC", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                        Text(
                            text = "${round.aiScore}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text("Normalized / 100", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "13 CANONICAL CATEGORIES BREAKDOWN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricCyan,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Category breakdown list
            val userCatMap = round.userJudgment?.categoryScores?.associateBy { it.category } ?: emptyMap()
            val aiCatMap = round.aiJudgment?.categoryScores?.associateBy { it.category } ?: emptyMap()

            val allCategories = (userCatMap.keys + aiCatMap.keys).distinct()

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(allCategories.toList()) { category ->
                    val userScore = userCatMap[category]?.score ?: 0
                    val aiScore = aiCatMap[category]?.score ?: 0
                    val weight = userCatMap[category]?.weight ?: aiCatMap[category]?.weight ?: 5

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ArenaBlack),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ArenaCardBorder))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$category ($weight%)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                Row {
                                    Text(
                                        text = "$userScore",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = BattleCrimson
                                    )
                                    Text(" vs ", fontSize = 12.sp, color = TextMuted)
                                    Text(
                                        text = "$aiScore",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = ElectricCyan
                                    )
                                    Text(" / 10", fontSize = 11.sp, color = TextMuted)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Dual progress indicators
                            LinearProgressIndicator(
                                progress = { (userScore / 10f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = BattleCrimson,
                                trackColor = ArenaCard
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            LinearProgressIndicator(
                                progress = { (aiScore / 10f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = ElectricCyan,
                                trackColor = ArenaCard
                            )
                        }
                    }
                }

                // Judge Rationale items
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "OFFICIAL JUDGE RATIONALE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldCrown,
                        letterSpacing = 1.sp
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ArenaCard)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("User Critique:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BattleCrimson)
                            Text(
                                text = round.userJudgment?.rationale ?: "No critique recorded.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cyber-MC Critique:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ElectricCyan)
                            Text(
                                text = round.aiJudgment?.rationale ?: "No critique recorded.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
