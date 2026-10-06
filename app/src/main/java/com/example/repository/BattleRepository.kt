package com.example.repository

import android.content.Context
import com.example.database.BattleEntity
import com.example.database.RapBattleDatabase
import com.example.engine.AiRapperEngine
import com.example.engine.JudgeEngine
import com.example.model.BattleConfig
import com.example.model.BattleEvent
import com.example.model.BattleMode
import com.example.model.BattleStats
import com.example.model.BattleStatus
import com.example.model.Competitor
import com.example.model.Difficulty
import com.example.model.Round
import com.example.model.RoundStatus
import com.example.model.ScoringSystem
import com.example.model.SubmissionStatus
import com.example.model.TranscriptEntry
import com.example.model.Verse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class BattleUiState(
    val battleId: String = UUID.randomUUID().toString(),
    val status: BattleStatus = BattleStatus.Waiting,
    val config: BattleConfig = BattleConfig(),
    val currentRoundNumber: Int = 1,
    val rounds: List<Round> = emptyList(),
    val timeRemainingSeconds: Int = 90,
    val isTimerActive: Boolean = false,
    val isAiThinking: Boolean = false,
    val isJudgingActive: Boolean = false,
    val activeRoundUserVerse: Verse? = null,
    val activeRoundAiVerse: Verse? = null,
    val currentRound: Round? = null,
    val transcript: List<TranscriptEntry> = emptyList(),
    val stats: BattleStats = BattleStats(),
    val winner: Competitor? = null,
    val finalUserScore: Double = 0.0,
    val finalAiScore: Double = 0.0,
    val latestEvent: BattleEvent? = null,
    val errorMessage: String? = null
)

class BattleRepository(private val context: Context) {

    private val db = RapBattleDatabase.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _uiState = MutableStateFlow(BattleUiState())
    val uiState: StateFlow<BattleUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        initializeNewBattle(BattleConfig())
    }

    fun getBattleHistory(): Flow<List<BattleEntity>> = db.battleDao().getAllBattles()

    fun initializeNewBattle(config: BattleConfig) {
        timerJob?.cancel()
        val battleId = UUID.randomUUID().toString()
        val initialRounds = (1..config.roundCount).map { roundNum ->
            Round(
                battleId = battleId,
                roundNumber = roundNum,
                roundWeight = if (roundNum == config.roundCount) config.finalRoundWeight else 1.0
            )
        }

        _uiState.value = BattleUiState(
            battleId = battleId,
            status = BattleStatus.UserTurn,
            config = config,
            currentRoundNumber = 1,
            rounds = initialRounds,
            timeRemainingSeconds = config.turnTimeoutSeconds,
            isTimerActive = true,
            isAiThinking = false,
            isJudgingActive = false,
            transcript = emptyList(),
            stats = BattleStats(totalRounds = config.roundCount),
            winner = null,
            latestEvent = BattleEvent(
                type = "BattleStarted",
                battleId = battleId,
                roundNumber = 1,
                actor = "System",
                message = "Battle initiated in ${config.mode.title} mode (${config.roundCount} rounds)."
            )
        )

        startAuthoritativeTimer()
    }

    private fun startAuthoritativeTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_uiState.value.timeRemainingSeconds > 0 && _uiState.value.status == BattleStatus.UserTurn) {
                delay(1000)
                _uiState.update { it.copy(timeRemainingSeconds = (it.timeRemainingSeconds - 1).coerceAtLeast(0)) }
            }

            if (_uiState.value.timeRemainingSeconds == 0 && _uiState.value.status == BattleStatus.UserTurn) {
                // Rule 13 & 44: Turn Timeout -> 0 score, AI still takes turn
                handleTurnTimeout()
            }
        }
    }

    private suspend fun handleTurnTimeout() {
        val state = _uiState.value
        val roundNum = state.currentRoundNumber

        val timeoutVerse = Verse(
            battleId = state.battleId,
            roundNumber = roundNum,
            competitor = Competitor.User,
            sequenceNumber = 1,
            content = "[TURN TIMED OUT - NO VERSE DELIVERED]",
            lineCount = 0,
            characterCount = 0,
            submissionStatus = SubmissionStatus.TimedOut,
            timedOut = true
        )

        _uiState.update {
            it.copy(
                status = BattleStatus.AiTurn,
                isTimerActive = false,
                activeRoundUserVerse = timeoutVerse,
                latestEvent = BattleEvent(
                    type = "VerseTimedOut",
                    battleId = state.battleId,
                    roundNumber = roundNum,
                    actor = "User",
                    message = "User turn timed out (90s limit reached). Received 0 score."
                )
            )
        }

        // Trigger AI turn
        triggerAiTurn(timeoutVerse)
    }

    fun submitUserVerse(rawContent: String): Boolean {
        val state = _uiState.value
        if (state.status != BattleStatus.UserTurn) return false

        val content = rawContent.trim()
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.isEmpty() || lines.size > state.config.verseMaximumLines || content.length > state.config.verseMaximumCharacters) {
            _uiState.update { it.copy(errorMessage = "Verse must be between 1 and ${state.config.verseMaximumLines} lines.") }
            return false
        }

        timerJob?.cancel()
        val roundNum = state.currentRoundNumber

        val userVerse = Verse(
            battleId = state.battleId,
            roundNumber = roundNum,
            competitor = Competitor.User,
            sequenceNumber = 1,
            content = content,
            lineCount = lines.size,
            characterCount = content.length,
            submissionStatus = SubmissionStatus.Accepted
        )

        _uiState.update {
            it.copy(
                status = BattleStatus.AiTurn,
                isTimerActive = false,
                activeRoundUserVerse = userVerse,
                errorMessage = null,
                latestEvent = BattleEvent(
                    type = "VerseSubmitted",
                    battleId = state.battleId,
                    roundNumber = roundNum,
                    actor = "User",
                    message = "User delivered ${lines.size} bars."
                )
            )
        }

        scope.launch {
            triggerAiTurn(userVerse)
        }

        return true
    }

    private suspend fun triggerAiTurn(userVerse: Verse) {
        val state = _uiState.value
        _uiState.update { it.copy(isAiThinking = true) }

        val previousBars = state.transcript.map { "${it.competitor}: ${it.content}" }
        val aiVerseText = AiRapperEngine.generateVerse(
            config = state.config,
            roundNumber = state.currentRoundNumber,
            userVerseText = userVerse.content,
            battleHistory = previousBars
        )

        val aiVerse = Verse(
            battleId = state.battleId,
            roundNumber = state.currentRoundNumber,
            competitor = Competitor.AI,
            sequenceNumber = 2,
            content = aiVerseText,
            lineCount = aiVerseText.lines().filter { it.isNotBlank() }.size,
            characterCount = aiVerseText.length,
            submissionStatus = SubmissionStatus.Accepted,
            generationProvider = "Gemini",
            generationModel = "gemini-3.5-flash"
        )

        _uiState.update {
            it.copy(
                isAiThinking = false,
                status = BattleStatus.Judging,
                isJudgingActive = true,
                activeRoundAiVerse = aiVerse,
                latestEvent = BattleEvent(
                    type = "AiVerseGenerated",
                    battleId = state.battleId,
                    roundNumber = state.currentRoundNumber,
                    actor = "AI",
                    message = "CYBER-MC delivered response verse."
                )
            )
        }

        // Trigger Independent Judging
        evaluateActiveRound(userVerse, aiVerse)
    }

    private suspend fun evaluateActiveRound(userVerse: Verse, aiVerse: Verse) {
        val state = _uiState.value
        val roundNum = state.currentRoundNumber
        val isFirstUserVerse = (roundNum == 1)

        val userJudgment = JudgeEngine.evaluateVerse(
            config = state.config,
            verse = userVerse,
            opponentVerse = null,
            roundNumber = roundNum,
            isFirstUserVerse = isFirstUserVerse
        )

        val aiJudgment = JudgeEngine.evaluateVerse(
            config = state.config,
            verse = aiVerse,
            opponentVerse = userVerse,
            roundNumber = roundNum,
            isFirstUserVerse = false
        )

        val userScore = userJudgment.normalizedScore
        val aiScore = aiJudgment.normalizedScore
        val roundWinner = ScoringSystem.determineRoundWinner(userScore, aiScore)

        val updatedRound = Round(
            battleId = state.battleId,
            roundNumber = roundNum,
            status = RoundStatus.Complete,
            userVerse = userVerse,
            aiVerse = aiVerse,
            userJudgment = userJudgment,
            aiJudgment = aiJudgment,
            userScore = userScore,
            aiScore = aiScore,
            roundWeight = if (roundNum == state.config.roundCount) state.config.finalRoundWeight else 1.0,
            winner = roundWinner,
            completedAt = System.currentTimeMillis()
        )

        val updatedRounds = state.rounds.map { if (it.roundNumber == roundNum) updatedRound else it }

        // Update transcript
        val newTranscript = state.transcript.toMutableList().apply {
            add(
                TranscriptEntry(
                    roundNumber = roundNum,
                    competitor = Competitor.User,
                    content = userVerse.content,
                    score = userScore,
                    isTimedOut = userVerse.timedOut,
                    judgeSummary = userJudgment.rationale
                )
            )
            add(
                TranscriptEntry(
                    roundNumber = roundNum,
                    competitor = Competitor.AI,
                    content = aiVerse.content,
                    score = aiScore,
                    isTimedOut = false,
                    judgeSummary = aiJudgment.rationale
                )
            )
        }

        // Update statistics
        val completedRounds = updatedRounds.filter { it.status == RoundStatus.Complete }
        val userWins = completedRounds.count { it.winner == Competitor.User }
        val aiWins = completedRounds.count { it.winner == Competitor.AI }
        val draws = completedRounds.count { it.winner == null }

        val newStats = state.stats.copy(
            userRoundsWon = userWins,
            aiRoundsWon = aiWins,
            drawnRounds = draws,
            userAverageScore = ScoringSystem.roundToTwoDecimals(completedRounds.map { it.userScore }.average()),
            aiAverageScore = ScoringSystem.roundToTwoDecimals(completedRounds.map { it.aiScore }.average()),
            userHighestScore = completedRounds.maxOfOrNull { it.userScore } ?: 0.0,
            aiHighestScore = completedRounds.maxOfOrNull { it.aiScore } ?: 0.0,
            totalVerses = newTranscript.size,
            timeoutCount = state.stats.timeoutCount + (if (userVerse.timedOut) 1 else 0)
        )

        val isFinalRound = (roundNum >= state.config.roundCount)

        if (isFinalRound) {
            // Battle is Complete! Calculate Final Weighted Scores & Authoritative Winner
            val (finalUser, finalAi, matchWinner) = ScoringSystem.calculateBattleFinalScores(
                rounds = completedRounds,
                totalConfiguredRounds = state.config.roundCount
            )

            val finalState = state.copy(
                status = BattleStatus.Completed,
                isJudgingActive = false,
                rounds = updatedRounds,
                currentRound = updatedRound,
                transcript = newTranscript,
                stats = newStats.copy(finalUserScore = finalUser, finalAiScore = finalAi),
                winner = matchWinner,
                finalUserScore = finalUser,
                finalAiScore = finalAi,
                latestEvent = BattleEvent(
                    type = "BattleCompleted",
                    battleId = state.battleId,
                    roundNumber = roundNum,
                    actor = "System",
                    message = "Battle complete! Winner: ${matchWinner?.name ?: "Draw"} ($finalUser vs $finalAi)."
                )
            )

            _uiState.value = finalState
            persistBattleToDatabase(finalState)
        } else {
            // Advance to next round
            _uiState.update {
                it.copy(
                    status = BattleStatus.RoundComplete,
                    isJudgingActive = false,
                    currentRoundNumber = roundNum,
                    currentRound = updatedRound,
                    rounds = updatedRounds,
                    transcript = newTranscript,
                    stats = newStats,
                    latestEvent = BattleEvent(
                        type = "RoundCompleted",
                        battleId = state.battleId,
                        roundNumber = roundNum,
                        actor = "System",
                        message = "Round $roundNum complete. Winner: ${roundWinner?.name ?: "Draw"} ($userScore vs $aiScore)."
                    )
                )
            }
        }
    }

    fun proceedToNextRound() {
        val state = _uiState.value
        if (state.status != BattleStatus.RoundComplete) return

        val nextRoundNum = state.currentRoundNumber + 1
        _uiState.update {
            it.copy(
                status = BattleStatus.UserTurn,
                currentRoundNumber = nextRoundNum,
                timeRemainingSeconds = it.config.turnTimeoutSeconds,
                isTimerActive = true,
                activeRoundUserVerse = null,
                activeRoundAiVerse = null,
                currentRound = it.rounds.firstOrNull { r -> r.roundNumber == nextRoundNum }
            )
        }

        startAuthoritativeTimer()
    }

    private fun persistBattleToDatabase(state: BattleUiState) {
        scope.launch {
            try {
                val transcriptArr = JSONArray().apply {
                    state.transcript.forEach { t ->
                        put(
                            JSONObject().apply {
                                put("round", t.roundNumber)
                                put("competitor", t.competitor.name)
                                put("score", t.score)
                                put("content", t.content)
                            }
                        )
                    }
                }

                val statsObj = JSONObject().apply {
                    put("finalUserScore", state.finalUserScore)
                    put("finalAiScore", state.finalAiScore)
                    put("userRoundsWon", state.stats.userRoundsWon)
                    put("aiRoundsWon", state.stats.aiRoundsWon)
                }

                val entity = BattleEntity(
                    id = state.battleId,
                    mode = state.config.mode.name,
                    difficulty = state.config.difficulty.name,
                    roundCount = state.config.roundCount,
                    theme = state.config.theme,
                    constraints = state.config.constraints.joinToString(","),
                    status = state.status.name,
                    currentRoundNumber = state.currentRoundNumber,
                    completedRounds = state.rounds.count { it.status == RoundStatus.Complete },
                    finalUserScore = state.finalUserScore,
                    finalAiScore = state.finalAiScore,
                    winner = state.winner?.name ?: "Draw",
                    transcriptJson = transcriptArr.toString(),
                    statsJson = statsObj.toString()
                )

                db.battleDao().insertBattle(entity)
            } catch (e: Exception) {
                // Log and continue
            }
        }
    }

    fun createRematch() {
        val currentConfig = _uiState.value.config
        initializeNewBattle(currentConfig)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun getMcpStateJson(): JSONObject {
        val s = _uiState.value
        return JSONObject().apply {
            put("battleId", s.battleId)
            put("status", s.status.name)
            put("currentRound", s.currentRoundNumber)
            put("roundCount", s.config.roundCount)
            put("mode", s.config.mode.name)
            put("difficulty", s.config.difficulty.name)
            put("theme", s.config.theme ?: "Freestyle")
            put("finalUserScore", s.finalUserScore)
            put("finalAiScore", s.finalAiScore)
            put("winner", s.winner?.name ?: "Undecided")
            put("timeRemainingSeconds", s.timeRemainingSeconds)
            put("transcriptCount", s.transcript.size)
        }
    }
}
