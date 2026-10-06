package com.example.model

import java.util.UUID

enum class BattleMode(val title: String, val description: String) {
    Classic(
        title = "Classic",
        description = "Standard 5-round battle. User starts, 90s timer, 1.5x final round multiplier."
    ),
    Freestyle(
        title = "Freestyle",
        description = "Unrestricted improvisation. Focus on rapid associations, wit, and punchlines."
    ),
    Themed(
        title = "Themed",
        description = "Mandatory chosen theme. Failure to stick to topic penalized in relevance."
    ),
    Constraint(
        title = "Constraint",
        description = "Explicit creative restrictions (rhyme schemes, required words, callbacks)."
    ),
    Championship(
        title = "Championship",
        description = "High stakes tournament mode with strict judging, deep analysis, and full transcript."
    )
}

enum class Difficulty(val title: String, val description: String) {
    Rookie("Rookie", "Relaxed pacing, straightforward end rhymes, gentle disses."),
    Competitor("Competitor", "Solid punchlines, internal rhymes, direct contextual rebuttals."),
    Elite("Elite", "Complex multisyllabic schemes, sharp counters, dynamic flow."),
    Champion("Champion", "Lethal double entendres, devastating callbacks, relentless technical skill.")
}

enum class Competitor {
    User,
    AI
}

enum class BattleStatus {
    Created,
    Waiting,
    UserTurn,
    AiTurn,
    Judging,
    RoundComplete,
    Completed,
    Cancelled,
    Failed
}

enum class TurnStatus {
    Pending,
    Active,
    Submitted,
    TimedOut,
    Failed,
    Cancelled
}

enum class SubmissionStatus {
    Accepted,
    Rejected,
    TimedOut,
    Failed
}

enum class RoundStatus {
    Pending,
    UserTurn,
    AiTurn,
    Judging,
    Complete,
    Failed
}

data class BattleConfig(
    val roundCount: Int = 5,
    val verseMinimumLines: Int = 1,
    val verseMaximumLines: Int = 16,
    val verseMaximumCharacters: Int = 16000,
    val turnTimeoutSeconds: Int = 90,
    val mode: BattleMode = BattleMode.Classic,
    val difficulty: Difficulty = Difficulty.Competitor,
    val theme: String? = null,
    val constraints: List<String> = emptyList(),
    val startingSide: Competitor = Competitor.User,
    val finalRoundWeight: Double = 1.5,
    val scoringVersion: String = "1.0.0",
    val judgeVersion: String = "1.0.0",
    val aiGenerationVersion: String = "1.0.0",
    val battleRulesVersion: String = "1.0.0"
)

data class Verse(
    val id: String = UUID.randomUUID().toString(),
    val battleId: String,
    val roundNumber: Int,
    val competitor: Competitor,
    val sequenceNumber: Int,
    val content: String,
    val lineCount: Int,
    val characterCount: Int,
    val submissionStatus: SubmissionStatus,
    val submittedAt: Long = System.currentTimeMillis(),
    val timedOut: Boolean = false,
    val generationProvider: String? = null,
    val generationModel: String? = null,
    val generationLatencyMs: Long? = null
)

data class CategoryScore(
    val category: String,
    val score: Int, // 0 to 10
    val weight: Int,
    val weightedContribution: Double,
    val explanation: String
)

data class Judgment(
    val id: String = UUID.randomUUID().toString(),
    val battleId: String,
    val roundNumber: Int,
    val verseId: String,
    val competitor: Competitor,
    val scoringVersion: String = "1.0.0",
    val judgeVersion: String = "1.0.0",
    val normalizedScore: Double,
    val rationale: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val categoryScores: List<CategoryScore>,
    val createdAt: Long = System.currentTimeMillis()
)

data class Round(
    val id: String = UUID.randomUUID().toString(),
    val battleId: String,
    val roundNumber: Int,
    val status: RoundStatus = RoundStatus.Pending,
    val userVerse: Verse? = null,
    val aiVerse: Verse? = null,
    val userJudgment: Judgment? = null,
    val aiJudgment: Judgment? = null,
    val userScore: Double = 0.0,
    val aiScore: Double = 0.0,
    val roundWeight: Double = 1.0,
    val winner: Competitor? = null, // null = Draw
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

data class BattleStats(
    val totalRounds: Int = 0,
    val userRoundsWon: Int = 0,
    val aiRoundsWon: Int = 0,
    val drawnRounds: Int = 0,
    val userAverageScore: Double = 0.0,
    val aiAverageScore: Double = 0.0,
    val userHighestScore: Double = 0.0,
    val aiHighestScore: Double = 0.0,
    val userPunchlineAvg: Double = 0.0,
    val aiPunchlineAvg: Double = 0.0,
    val userRhymeAvg: Double = 0.0,
    val aiRhymeAvg: Double = 0.0,
    val userRebuttalAvg: Double = 0.0,
    val aiRebuttalAvg: Double = 0.0,
    val totalVerses: Int = 0,
    val timeoutCount: Int = 0,
    val finalUserScore: Double = 0.0,
    val finalAiScore: Double = 0.0
)

data class TranscriptEntry(
    val roundNumber: Int,
    val competitor: Competitor,
    val content: String,
    val score: Double,
    val isTimedOut: Boolean,
    val judgeSummary: String
)

data class BattleEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val battleId: String,
    val roundNumber: Int,
    val actor: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
