package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BattleMode
import com.example.model.BattleStatus
import com.example.model.Competitor
import com.example.model.Round
import com.example.repository.BattleUiState
import com.example.ui.theme.*

@Composable
fun ArenaScreen(
    uiState: BattleUiState,
    onSubmitVerse: (String) -> Unit,
    onProceedNextRound: () -> Unit,
    onViewScorecard: (Round) -> Unit,
    onViewFinalResults: () -> Unit,
    onQuitBattle: () -> Unit
) {
    var verseInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Calculate line count and character count
    val lineCount = remember(verseInput) {
        if (verseInput.isBlank()) 0 else verseInput.lines().count { it.isNotBlank() }
    }
    val charCount = remember(verseInput) { verseInput.length }

    val isFinalRound = (uiState.currentRoundNumber == uiState.config.roundCount)
    val isUserTurn = (uiState.status == BattleStatus.UserTurn)

    // Pulsing animation for active timer
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Audio beat simulated toggle
    var isBeatPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.transcript.size) {
        if (uiState.transcript.isNotEmpty()) {
            listState.animateScrollToItem(uiState.transcript.size)
        }
    }

    Scaffold(
        containerColor = ArenaBlack,
        topBar = {
            // Battle Arena Top Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ArenaSurface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onQuitBattle, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isFinalRound) "FINAL ROUND (1.5x WEIGHT)" else "ROUND ${uiState.currentRoundNumber} OF ${uiState.config.roundCount}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isFinalRound) GoldCrown else BattleCrimson
                                )
                            }
                            Text(
                                text = "${uiState.config.mode.name} • ${uiState.config.difficulty.name}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Modular Beat Player Toggle (Rule 80)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isBeatPlaying = !isBeatPlaying },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isBeatPlaying) NeonPurple else ArenaCard)
                        ) {
                            Icon(
                                imageVector = if (isBeatPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                contentDescription = "Beat Track",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 90-Second Authoritative Timer Indicator
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        uiState.timeRemainingSeconds <= 15 -> ErrorRed
                                        uiState.timeRemainingSeconds <= 30 -> WarningAmber
                                        else -> ArenaCard
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .then(if (uiState.timeRemainingSeconds <= 15) Modifier.scale(pulseScale) else Modifier)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = "Timer",
                                    tint = if (uiState.timeRemainingSeconds <= 30) ArenaBlack else ElectricCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${uiState.timeRemainingSeconds}s",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (uiState.timeRemainingSeconds <= 30) ArenaBlack else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Live Status Banner
            AnimatedVisibility(visible = uiState.isAiThinking) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = ArenaCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ElectricCyan, NeonPurple)))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ElectricCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "CYBER-MC is crafting dynamic rebuttal bars...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = ElectricCyan
                        )
                    }
                }
            }

            AnimatedVisibility(visible = uiState.isJudgingActive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = ArenaCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GoldCrown, BattleCrimson)))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = GoldCrown,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Head Judge evaluating 13 canonical categories...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = GoldCrown
                        )
                    }
                }
            }

            // Transcript Scroll Area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // Round Header / Theme Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ArenaCard),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(ArenaCardBorder, ArenaCardBorder)))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "THEME: ${uiState.config.theme ?: "Freestyle Arena"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                                if (uiState.config.constraints.isNotEmpty()) {
                                    Text(
                                        text = "${uiState.config.constraints.size} Constraints",
                                        fontSize = 11.sp,
                                        color = WarningAmber
                                    )
                                }
                            }
                            if (uiState.config.constraints.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Rules: ${uiState.config.constraints.joinToString("; ")}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // Transcript Verses
                items(uiState.transcript) { entry ->
                    val isUser = entry.competitor == Competitor.User
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isUser) BattleCrimson.copy(alpha = 0.5f) else ElectricCyan.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) ArenaSurface else ArenaCard
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isUser) BattleCrimson else ElectricCyan),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isUser) "U" else "AI",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = ArenaBlack
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isUser) "YOU (Round ${entry.roundNumber})" else "CYBER-MC (Round ${entry.roundNumber})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isUser) BattleCrimson else ElectricCyan
                                    )
                                }

                                SuggestionChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            text = "${entry.score} pts",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isUser) BattleCrimsonDark else ElectricCyanDark
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = entry.content,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                lineHeight = 20.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            if (entry.judgeSummary.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Judge: ${entry.judgeSummary}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }

                // Active Round user verse if not in transcript yet
                if (uiState.activeRoundUserVerse != null && uiState.transcript.none { it.roundNumber == uiState.currentRoundNumber && it.competitor == Competitor.User }) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BattleCrimson, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = ArenaSurface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "YOU (Round ${uiState.currentRoundNumber})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BattleCrimson
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = uiState.activeRoundUserVerse.content,
                                    fontSize = 14.sp,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Turn Controller / Verse Input
            when (uiState.status) {
                BattleStatus.UserTurn -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        // Quick Inspiration Prompts
                        val inspirationBars = listOf(
                            "Flipping your cadence with algorithmic heat,",
                            "You talk about kings but you're facing defeat!",
                            "Lethal punchlines with no room to retreat,",
                            "Stepping in the arena, bringing pure elite!"
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            items(inspirationBars) { line ->
                                SuggestionChip(
                                    onClick = {
                                        verseInput = if (verseInput.isBlank()) line else "$verseInput\n$line"
                                    },
                                    label = { Text(line.take(28) + "...", fontSize = 11.sp, color = TextSecondary) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = ArenaCard),
                                    border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = ArenaCardBorder)
                                )
                            }
                        }

                        // Verse Input Field
                        OutlinedTextField(
                            value = verseInput,
                            onValueChange = { verseInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 150.dp)
                                .testTag("verse_input_field"),
                            placeholder = {
                                Text(
                                    "Drop your bars here (1 to 16 lines)...\nMake every punchline count!",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ArenaCard,
                                unfocusedContainerColor = ArenaSurface,
                                focusedBorderColor = BattleCrimson,
                                unfocusedBorderColor = ArenaCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Counters & Submit Action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row {
                                Text(
                                    text = "$lineCount / 16 lines",
                                    fontSize = 11.sp,
                                    color = if (lineCount in 1..16) ElectricCyan else TextMuted
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "$charCount / 16k chars",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            Button(
                                onClick = {
                                    if (verseInput.isNotBlank()) {
                                        onSubmitVerse(verseInput)
                                        verseInput = ""
                                    }
                                },
                                enabled = verseInput.isNotBlank() && lineCount in 1..16,
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("submit_verse_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BattleCrimson,
                                    disabledContainerColor = ArenaCard
                                )
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("DROP VERSE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                BattleStatus.RoundComplete -> {
                    // Round Result Card
                    val currentRound = uiState.currentRound
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = ArenaCard),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(listOf(BattleCrimson, ElectricCyan))
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "ROUND ${uiState.currentRoundNumber} WINNER",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = when (currentRound?.winner) {
                                            Competitor.User -> "YOU WIN THIS ROUND!"
                                            Competitor.AI -> "CYBER-MC WINS THIS ROUND"
                                            else -> "ROUND TIED (DRAW)"
                                        },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = when (currentRound?.winner) {
                                            Competitor.User -> BattleCrimson
                                            Competitor.AI -> ElectricCyan
                                            else -> GoldCrown
                                        }
                                    )
                                }

                                Text(
                                    text = "${currentRound?.userScore ?: 0.0} - ${currentRound?.aiScore ?: 0.0}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { if (currentRound != null) onViewScorecard(currentRound) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scorecard", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = onProceedNextRound,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BattleCrimson)
                                ) {
                                    Text("Next Round", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                BattleStatus.Completed -> {
                    // Match Over CTA
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = ArenaCard),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(listOf(GoldCrown, BattleCrimson))
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "BATTLE COMPLETED!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = GoldCrown
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Final Weighted Score: ${uiState.finalUserScore} (You) vs ${uiState.finalAiScore} (Cyber-MC)",
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onViewFinalResults,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldCrown, contentColor = ArenaBlack)
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("VIEW FINAL RESULTS & STATS", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                else -> {
                    // Neutral / Waiting state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cypher in progress...",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
