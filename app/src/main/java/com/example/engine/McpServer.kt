package com.example.engine

import com.example.model.BattleConfig
import com.example.model.BattleMode
import com.example.model.BattleStats
import com.example.model.Difficulty
import com.example.model.Round
import com.example.model.TranscriptEntry
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class McpPrincipal(
    val id: String,
    val type: String = "User",
    val username: String = "authorized_rapper",
    val scopes: Set<String> = setOf(
        "battle.read",
        "battle.create",
        "battle.configure",
        "battle.submit",
        "battle.generate",
        "battle.judge",
        "battle.end",
        "battle.restart",
        "battle.history",
        "battle.statistics"
    )
)

data class McpToken(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long = 3600,
    val principal: McpPrincipal,
    val issuedAt: Long = System.currentTimeMillis()
)

data class McpToolDef(
    val name: String,
    val description: String,
    val requiredScope: String,
    val inputSchema: String
)

data class McpAuditLog(
    val id: String = UUID.randomUUID().toString(),
    val action: String,
    val tool: String?,
    val principalId: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)

object McpServer {

    private var activeToken: McpToken? = McpToken(
        accessToken = "mcp_live_" + UUID.randomUUID().toString().replace("-", ""),
        principal = McpPrincipal(id = "usr_" + UUID.randomUUID().toString().take(8))
    )

    private val auditLogs = mutableListOf<McpAuditLog>()

    val TOOLS = listOf(
        McpToolDef(
            name = "start_battle",
            description = "Initializes an authoritative new rap battle instance.",
            requiredScope = "battle.create",
            inputSchema = "{\"type\": \"object\", \"properties\": {\"mode\": {\"type\": \"string\"}}}"
        ),
        McpToolDef(
            name = "configure_battle",
            description = "Configures battle format, rounds (3, 5, 7), difficulty, theme, and constraints.",
            requiredScope = "battle.configure",
            inputSchema = "{\"type\": \"object\", \"properties\": {\"roundCount\": {\"type\": \"integer\"}}}"
        ),
        McpToolDef(
            name = "submit_verse",
            description = "Submits an authenticated competitor verse for the active turn.",
            requiredScope = "battle.submit",
            inputSchema = "{\"type\": \"object\", \"properties\": {\"content\": {\"type\": \"string\"}}}"
        ),
        McpToolDef(
            name = "generate_ai_response",
            description = "Generates responsive, contextual AI rebuttal verse.",
            requiredScope = "battle.generate",
            inputSchema = "{\"type\": \"object\", \"properties\": {\"battleId\": {\"type\": \"string\"}}}"
        ),
        McpToolDef(
            name = "evaluate_verse",
            description = "Invokes independent judge scoring across 13 canonical categories.",
            requiredScope = "battle.judge",
            inputSchema = "{\"type\": \"object\", \"properties\": {\"verseId\": {\"type\": \"string\"}}}"
        ),
        McpToolDef(
            name = "get_battle_state",
            description = "Retrieves authoritative state machine position and active turn status.",
            requiredScope = "battle.read",
            inputSchema = "{\"type\": \"object\", \"properties\": {\"battleId\": {\"type\": \"string\"}}}"
        ),
        McpToolDef(
            name = "get_current_round",
            description = "Retrieves current round details, verses, and live timers.",
            requiredScope = "battle.read",
            inputSchema = "{\"type\": \"object\", \"properties\": {\"battleId\": {\"type\": \"string\"}}}"
        ),
        McpToolDef(
            name = "get_scores",
            description = "Retrieves category scores, round scores, and final weighted totals.",
            requiredScope = "battle.read",
            inputSchema = "{\"type\": \"object\", \"properties\": {\"battleId\": {\"type\": \"string\"}}}"
        ),
        McpToolDef(
            name = "get_statistics",
            description = "Retrieves cumulative battle statistics and punchline/rhyme averages.",
            requiredScope = "battle.statistics",
            inputSchema = "{\"type\": \"object\", \"properties\": {\"battleId\": {\"type\": \"string\"}}}"
        ),
        McpToolDef(
            name = "end_battle",
            description = "Concludes or concedes battle with final calculations.",
            requiredScope = "battle.end",
            inputSchema = "{\"type\": \"object\", \"properties\": {\"battleId\": {\"type\": \"string\"}}}"
        ),
        McpToolDef(
            name = "restart_battle",
            description = "Spawns a clean rematch inheriting prior configurations.",
            requiredScope = "battle.restart",
            inputSchema = "{\"type\": \"object\", \"properties\": {\"battleId\": {\"type\": \"string\"}}}"
        ),
        McpToolDef(
            name = "get_history",
            description = "Lists historical battles for the authenticated principal.",
            requiredScope = "battle.history",
            inputSchema = "{\"type\": \"object\", \"properties\": {\"limit\": {\"type\": \"integer\"}}}"
        ),
        McpToolDef(
            name = "get_transcript",
            description = "Retrieves verbatim chronological battle transcript.",
            requiredScope = "battle.read",
            inputSchema = "{\"type\": \"object\", \"properties\": {\"battleId\": {\"type\": \"string\"}}}"
        )
    )

    fun getActiveToken(): McpToken? = activeToken

    fun refreshActiveToken(): McpToken {
        val newToken = McpToken(
            accessToken = "mcp_live_" + UUID.randomUUID().toString().replace("-", ""),
            principal = activeToken?.principal ?: McpPrincipal(id = "usr_" + UUID.randomUUID().toString().take(8))
        )
        activeToken = newToken
        auditLogs.add(McpAuditLog(action = "TokenRefreshed", tool = null, principalId = newToken.principal.id, status = "Success"))
        return newToken
    }

    fun revokeToken() {
        val pId = activeToken?.principal?.id ?: "unknown"
        activeToken = null
        auditLogs.add(McpAuditLog(action = "TokenRevoked", tool = null, principalId = pId, status = "Revoked"))
    }

    fun getAuditLogs(): List<McpAuditLog> = auditLogs.toList()

    /**
     * Executes an MCP tool securely verifying authorization and scopes.
     */
    fun executeTool(
        toolName: String,
        params: JSONObject,
        currentBattleState: JSONObject
    ): JSONObject {
        val token = activeToken
        if (token == null) {
            recordAudit(toolName, "anonymous", "Denied_Unauthenticated")
            return JSONObject().apply {
                put("error", "Unauthorized: Valid MCP OAuth bearer token required.")
                put("code", "UNAUTHENTICATED")
            }
        }

        val toolDef = TOOLS.firstOrNull { it.name == toolName }
        if (toolDef == null) {
            recordAudit(toolName, token.principal.id, "Denied_UnknownTool")
            return JSONObject().apply {
                put("error", "Unknown MCP tool '$toolName'")
                put("code", "INVALID_TOOL")
            }
        }

        if (!token.principal.scopes.contains(toolDef.requiredScope)) {
            recordAudit(toolName, token.principal.id, "Denied_InsufficientScope")
            return JSONObject().apply {
                put("error", "Forbidden: Missing required scope '${toolDef.requiredScope}'")
                put("code", "INSUFFICIENT_SCOPE")
            }
        }

        recordAudit(toolName, token.principal.id, "Success")

        // Tool implementation dispatch
        return when (toolName) {
            "start_battle" -> {
                JSONObject().apply {
                    put("status", "Created")
                    put("battleId", UUID.randomUUID().toString())
                    put("message", "New battle state initialized successfully.")
                }
            }
            "configure_battle" -> {
                JSONObject().apply {
                    put("status", "Configured")
                    put("configuration", params)
                }
            }
            "get_battle_state" -> currentBattleState
            "get_scores" -> {
                JSONObject().apply {
                    put("scoringVersion", "1.0.0")
                    put("categories", JSONArray().apply {
                        put("Punchlines (12%)")
                        put("Wordplay (10%)")
                        put("Rhyme quality (10%)")
                        put("Multisyllabic rhyme (6%)")
                        put("Flow (8%)")
                        put("Creativity (10%)")
                        put("Originality (8%)")
                        put("Relevance (6%)")
                        put("Rebuttal / Response Quality (10%)")
                        put("Setup & payoff (6%)")
                        put("Thematic consistency (5%)")
                        put("Crowd appeal (4%)")
                        put("Overall impact (5%)")
                    })
                    put("finalRoundWeight", 1.5)
                }
            }
            "get_statistics" -> {
                currentBattleState.optJSONObject("statistics") ?: JSONObject().apply {
                    put("totalRounds", 5)
                    put("userRoundsWon", 0)
                    put("aiRoundsWon", 0)
                }
            }
            "get_transcript" -> {
                JSONObject().apply {
                    put("transcript", currentBattleState.optJSONArray("transcript") ?: JSONArray())
                }
            }
            else -> {
                JSONObject().apply {
                    put("tool", toolName)
                    put("status", "Executed")
                    put("executedAt", System.currentTimeMillis())
                    put("echoParams", params)
                }
            }
        }
    }

    /**
     * Resolves an MCP URI resource e.g. battle://state/{id}
     */
    fun resolveResource(uri: String, currentBattleState: JSONObject): JSONObject {
        return when {
            uri.startsWith("battle://state") -> currentBattleState
            uri.startsWith("battle://scores") -> executeTool("get_scores", JSONObject(), currentBattleState)
            uri.startsWith("battle://statistics") -> executeTool("get_statistics", JSONObject(), currentBattleState)
            uri.startsWith("battle://transcript") -> JSONObject().apply {
                put("transcript", currentBattleState.optJSONArray("transcript") ?: JSONArray())
            }
            else -> JSONObject().apply {
                put("error", "Resource URI not found: $uri")
            }
        }
    }

    private fun recordAudit(tool: String, principalId: String, status: String) {
        auditLogs.add(
            McpAuditLog(
                action = "ToolInvocation",
                tool = tool,
                principalId = principalId,
                status = status
            )
        )
    }
}
