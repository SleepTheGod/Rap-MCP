package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BattleConfig
import com.example.model.BattleMode
import com.example.model.Difficulty
import com.example.ui.theme.*

@Composable
fun LobbyScreen(
    currentConfig: BattleConfig,
    onStartBattle: (BattleConfig) -> Unit,
    onNavigateToMcp: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentConfig.mode) }
    var selectedRoundCount by remember { mutableIntStateOf(currentConfig.roundCount) }
    var selectedDifficulty by remember { mutableStateOf(currentConfig.difficulty) }
    var themeText by remember { mutableStateOf(currentConfig.theme ?: "Tech & AI Dominance") }
    var constraintsText by remember { mutableStateOf(currentConfig.constraints.joinToString(", ").ifEmpty { "Must include callback, AABB rhyme scheme" }) }

    val presetThemes = listOf(
        "Tech & AI Dominance",
        "Street Legends",
        "Cyberpunk 2099",
        "Classic 90s Golden Era",
        "Cosmic Frontiers",
        "Gaming & Speedrunning"
    )

    val presetConstraints = listOf(
        "AABB Rhyme Scheme",
        "Forbidden word: 'Mic'",
        "Must quote opponent",
        "Multisyllabic End Rhymes",
        "Metaphor Challenge"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(BattleCrimsonDark, ArenaSurface, ElectricCyanDark)
                        )
                    )
                    .border(1.dp, ArenaCardBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Mic",
                                tint = ElectricCyan,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "MCP RAP BATTLE",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 1.sp
                            )
                        }

                        AssistChip(
                            onClick = onNavigateToMcp,
                            label = { Text("MCP Live", fontSize = 11.sp, color = ElectricCyan) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = "MCP",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(containerColor = ArenaCard)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Authoritative competitive rap battles against Cyber-MC. 13-category scoring with 1.5x final round multiplier.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Section: Battle Mode
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SELECT BATTLE MODE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan,
                    letterSpacing = 1.sp
                )

                BattleMode.entries.forEach { mode ->
                    val isSelected = selectedMode == mode
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMode = mode
                                if (mode == BattleMode.Themed && themeText.isBlank()) {
                                    themeText = "Tech & AI Dominance"
                                }
                            }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) BattleCrimson else ArenaCardBorder,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) ArenaCard else ArenaSurface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) BattleCrimson else TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = mode.description,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedMode = mode },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BattleCrimson,
                                    unselectedColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section: Round Count
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ROUNDS (FINAL ROUND 1.5x WEIGHT)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(3, 5, 7).forEach { rounds ->
                        val isSelected = selectedRoundCount == rounds
                        OutlinedButton(
                            onClick = { selectedRoundCount = rounds },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) BattleCrimson else ArenaCard,
                                contentColor = if (isSelected) TextPrimary else TextSecondary
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(
                                    if (isSelected) listOf(BattleCrimson, BattleCrimson)
                                    else listOf(ArenaCardBorder, ArenaCardBorder)
                                )
                            )
                        ) {
                            Text(
                                text = "$rounds Rounds",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Section: Difficulty
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AI OPPONENT DIFFICULTY",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan,
                    letterSpacing = 1.sp
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(Difficulty.entries) { diff ->
                        val isSelected = selectedDifficulty == diff
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDifficulty = diff },
                            label = { Text(diff.title, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldCrown,
                                selectedLabelColor = ArenaBlack,
                                containerColor = ArenaCard,
                                labelColor = TextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) GoldCrown else ArenaCardBorder
                            )
                        )
                    }
                }

                Text(
                    text = selectedDifficulty.description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // Section: Theme (Mandatory for Themed mode)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "THEME " + if (selectedMode == BattleMode.Themed) "(MANDATORY)" else "(OPTIONAL)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedMode == BattleMode.Themed) WarningAmber else ElectricCyan,
                        letterSpacing = 1.sp
                    )
                }

                OutlinedTextField(
                    value = themeText,
                    onValueChange = { themeText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text("e.g. Cyberpunk, Street Legends, Artificial Intelligence") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ArenaCard,
                        unfocusedContainerColor = ArenaSurface,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = ArenaCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presetThemes) { pTheme ->
                        SuggestionChip(
                            onClick = { themeText = pTheme },
                            label = { Text(pTheme, fontSize = 11.sp, color = TextSecondary) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = ArenaCard),
                            border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = ArenaCardBorder)
                        )
                    }
                }
            }
        }

        // Section: Constraints (Mandatory for Constraint mode)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CREATIVE CONSTRAINTS " + if (selectedMode == BattleMode.Constraint) "(MANDATORY)" else "(OPTIONAL)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedMode == BattleMode.Constraint) WarningAmber else ElectricCyan,
                    letterSpacing = 1.sp
                )

                OutlinedTextField(
                    value = constraintsText,
                    onValueChange = { constraintsText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text("Comma-separated constraints e.g. AABB rhyme, Required word: Cypher") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ArenaCard,
                        unfocusedContainerColor = ArenaSurface,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = ArenaCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presetConstraints) { pConstraint ->
                        SuggestionChip(
                            onClick = {
                                constraintsText = if (constraintsText.isBlank()) pConstraint else "$constraintsText, $pConstraint"
                            },
                            label = { Text(pConstraint, fontSize = 11.sp, color = TextSecondary) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = ArenaCard),
                            border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = ArenaCardBorder)
                        )
                    }
                }
            }
        }

        // Start Battle CTA
        item {
            Button(
                onClick = {
                    val finalConstraints = constraintsText
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    val finalConfig = BattleConfig(
                        mode = selectedMode,
                        roundCount = selectedRoundCount,
                        difficulty = selectedDifficulty,
                        theme = themeText.trim().ifEmpty { null },
                        constraints = finalConstraints,
                        turnTimeoutSeconds = 90,
                        finalRoundWeight = 1.5
                    )
                    onStartBattle(finalConfig)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_battle_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BattleCrimson,
                    contentColor = TextPrimary
                )
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ENTER BATTLE ARENA",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }

        // Production Developer Export Link
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = onNavigateToExport) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Inspect Production Stack & Export ZIP",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
