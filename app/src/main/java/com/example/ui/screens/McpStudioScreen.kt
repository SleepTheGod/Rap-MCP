package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.McpServer
import com.example.ui.theme.*
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpStudioScreen(
    currentBattleState: JSONObject,
    onBack: () -> Unit
) {
    var activeToken by remember { mutableStateOf(McpServer.getActiveToken()) }
    var selectedToolName by remember { mutableStateOf("get_battle_state") }
    var toolParamInput by remember { mutableStateOf("{}") }
    var executionResultJson by remember { mutableStateOf<String?>(null) }
    var auditLogs by remember { mutableStateOf(McpServer.getAuditLogs()) }

    Scaffold(
        containerColor = ArenaBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "MCP PROTOCOL SERVER",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // OAuth PKCE & Principal Details
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ArenaCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(ElectricCyan.copy(alpha = 0.5f))
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AUTH STATUS: " + if (activeToken != null) "AUTHORIZED" else "REVOKED",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeToken != null) SuccessGreen else ErrorRed
                            )

                            Row {
                                TextButton(
                                    onClick = {
                                        activeToken = McpServer.refreshActiveToken()
                                        auditLogs = McpServer.getAuditLogs()
                                    }
                                ) {
                                    Text("Rotate Token", fontSize = 11.sp, color = ElectricCyan)
                                }

                                TextButton(
                                    onClick = {
                                        McpServer.revokeToken()
                                        activeToken = null
                                        auditLogs = McpServer.getAuditLogs()
                                    }
                                ) {
                                    Text("Revoke", fontSize = 11.sp, color = ErrorRed)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Principal: ${activeToken?.principal?.username ?: "None"} (ID: ${activeToken?.principal?.id ?: "N/A"})",
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Bearer: ${activeToken?.accessToken?.take(20) ?: "None"}...",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Granted Scopes (10):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        val scopes = activeToken?.principal?.scopes?.toList() ?: emptyList()
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(scopes) { scope ->
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text(scope, fontSize = 10.sp, color = ElectricCyan) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = ArenaSurface)
                                )
                            }
                        }
                    }
                }
            }

            // 13 Tools Execution Console
            item {
                Text(
                    text = "EXECUTE 13 CANONICAL MCP TOOLS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan,
                    letterSpacing = 1.sp
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(McpServer.TOOLS) { tool ->
                        val isSelected = (selectedToolName == tool.name)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedToolName = tool.name
                                toolParamInput = when (tool.name) {
                                    "start_battle" -> "{\"mode\": \"Classic\"}"
                                    "configure_battle" -> "{\"roundCount\": 5, \"difficulty\": \"Elite\"}"
                                    "submit_verse" -> "{\"content\": \"Spitting sharp syllables in the live arena!\"}"
                                    else -> "{}"
                                }
                            },
                            label = { Text(tool.name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BattleCrimson,
                                selectedLabelColor = TextPrimary,
                                containerColor = ArenaCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            // Tool Parameters and Execute CTA
            item {
                val currentTool = McpServer.TOOLS.firstOrNull { it.name == selectedToolName }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ArenaCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = currentTool?.description ?: "",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Required Scope: ${currentTool?.requiredScope}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = toolParamInput,
                            onValueChange = { toolParamInput = it },
                            label = { Text("Input Parameters (JSON)", fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp, max = 100.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TextPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ArenaSurface,
                                unfocusedContainerColor = ArenaSurface,
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = ArenaCardBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val paramsObj = try {
                                    JSONObject(toolParamInput)
                                } catch (e: Exception) {
                                    JSONObject()
                                }
                                val result = McpServer.executeTool(
                                    toolName = selectedToolName,
                                    params = paramsObj,
                                    currentBattleState = currentBattleState
                                )
                                executionResultJson = result.toString(2)
                                auditLogs = McpServer.getAuditLogs()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ArenaBlack)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RUN TOOL (${selectedToolName})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Output JSON display
            if (executionResultJson != null) {
                item {
                    Text(
                        text = "TOOL RESPONSE (JSON):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ArenaBlack),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(ArenaCardBorder)
                        )
                    ) {
                        Text(
                            text = executionResultJson ?: "",
                            modifier = Modifier.padding(12.dp),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = SuccessGreen,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Audit Logs
            item {
                Text(
                    text = "MCP AUDIT TRAIL",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan,
                    letterSpacing = 1.sp
                )
            }

            items(auditLogs.takeLast(6).reversed()) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ArenaCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${log.action} • ${log.tool ?: "Auth"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Principal: ${log.principalId}",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }

                        SuggestionChip(
                            onClick = { },
                            label = { Text(log.status, fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (log.status == "Success") ArenaSurface else ErrorRed.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }
    }
}
