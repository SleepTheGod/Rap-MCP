package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.McpServer
import com.example.model.BattleConfig
import com.example.model.BattleMode
import com.example.model.Competitor
import com.example.model.Difficulty
import com.example.model.Round
import com.example.model.RoundStatus
import com.example.model.ScoringSystem
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MCP Rap Battle", appName)
    }

    @Test
    fun `canonical scoring categories sum to exactly 100 percent`() {
        val totalWeight = ScoringSystem.CATEGORIES.sumOf { it.weight }
        assertEquals(100, totalWeight)
        assertEquals(13, ScoringSystem.CATEGORIES.size)
    }

    @Test
    fun `first user turn uses response quality instead of rebuttal`() {
        val scores = mapOf("Response Quality" to 8, "Punchlines" to 8)
        val (_, breakdown) = ScoringSystem.calculateVerseScore(
            scores = scores,
            isFirstUserVerse = true
        )
        val hasResponseQuality = breakdown.any { it.category == "Response Quality" }
        assertTrue("First user verse must contain 'Response Quality'", hasResponseQuality)
    }

    @Test
    fun `final round weighting applies 1 point 5 multiplier`() {
        // 5-round battle: R1..R4 weight = 1.0, R5 weight = 1.5. Total weights = 5.5
        val rounds = listOf(
            Round(battleId = "b1", roundNumber = 1, userScore = 80.0, aiScore = 70.0, roundWeight = 1.0, status = RoundStatus.Complete),
            Round(battleId = "b1", roundNumber = 2, userScore = 80.0, aiScore = 70.0, roundWeight = 1.0, status = RoundStatus.Complete),
            Round(battleId = "b1", roundNumber = 3, userScore = 80.0, aiScore = 70.0, roundWeight = 1.0, status = RoundStatus.Complete),
            Round(battleId = "b1", roundNumber = 4, userScore = 80.0, aiScore = 70.0, roundWeight = 1.0, status = RoundStatus.Complete),
            Round(battleId = "b1", roundNumber = 5, userScore = 80.0, aiScore = 70.0, roundWeight = 1.5, status = RoundStatus.Complete)
        )

        val (finalUser, finalAi, winner) = ScoringSystem.calculateBattleFinalScores(rounds, 5)
        assertEquals(80.0, finalUser, 0.01)
        assertEquals(70.0, finalAi, 0.01)
        assertEquals(Competitor.User, winner)
    }

    @Test
    fun `final tiebreaker compares final round score`() {
        // Final battle score equals, but User wins final round
        val rounds = listOf(
            Round(battleId = "b1", roundNumber = 1, userScore = 70.0, aiScore = 85.0, roundWeight = 1.0, status = RoundStatus.Complete),
            Round(battleId = "b1", roundNumber = 2, userScore = 70.0, aiScore = 85.0, roundWeight = 1.0, status = RoundStatus.Complete),
            Round(battleId = "b1", roundNumber = 3, userScore = 90.0, aiScore = 70.0, roundWeight = 1.5, status = RoundStatus.Complete)
        )
        // User: (70 + 70 + 90*1.5) / 3.5 = (140 + 135) / 3.5 = 275 / 3.5 = 78.57
        // AI: (85 + 85 + 70*1.5) / 3.5 = (170 + 105) / 3.5 = 275 / 3.5 = 78.57
        val (finalUser, finalAi, winner) = ScoringSystem.calculateBattleFinalScores(rounds, 3)
        assertEquals(finalUser, finalAi, 0.01)
        // User scored higher in Round 3 (90 vs 70), so tiebreaker declares User winner
        assertEquals(Competitor.User, winner)
    }

    @Test
    fun `mcp server provides 13 tools and valid token authorization`() {
        val tools = McpServer.TOOLS
        assertEquals(13, tools.size)

        val token = McpServer.getActiveToken()
        assertNotNull(token)
        assertTrue(token!!.principal.scopes.contains("battle.read"))
        assertTrue(token.principal.scopes.contains("battle.submit"))

        val result = McpServer.executeTool(
            toolName = "get_scores",
            params = JSONObject(),
            currentBattleState = JSONObject().apply { put("battleId", "test_id") }
        )
        assertEquals("1.0.0", result.optString("scoringVersion"))
    }
}
