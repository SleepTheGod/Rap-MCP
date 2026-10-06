package com.example.model

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

object ScoringSystem {

    val CATEGORIES = listOf(
        CategoryDef("Punchlines", 12, "Setup, surprise, impact, precision, and memorability"),
        CategoryDef("Wordplay", 10, "Puns, double entendres, homophones, and figurative language"),
        CategoryDef("Rhyme quality", 10, "End rhymes, internal rhymes, and phonetic crispness"),
        CategoryDef("Multisyllabic rhyme quality", 6, "Complexity, multi-syllable matching, and natural delivery"),
        CategoryDef("Flow", 8, "Rhythmic variation, cadence, syllabic balance, and line structure"),
        CategoryDef("Creativity", 10, "Quality of concepts, distinctive angles, and execution"),
        CategoryDef("Originality", 8, "Freshness and non-derivative execution within the battle"),
        CategoryDef("Relevance", 6, "Engagement with battle context, theme, and opponent"),
        CategoryDef("Rebuttal quality", 10, "Direct counters, reframing, and opponent callbacks (Response Quality for R1)"),
        CategoryDef("Setup and payoff", 6, "Structural build-up delivering punchline effectiveness"),
        CategoryDef("Thematic consistency", 5, "Adherence to chosen theme, constraints, and battle premise"),
        CategoryDef("Crowd appeal", 4, "Performance energy, charisma, and entertainment value"),
        CategoryDef("Overall impact", 5, "Holistic battle execution and competitive punch")
    )

    data class CategoryDef(
        val name: String,
        val weight: Int,
        val description: String
    )

    fun roundToTwoDecimals(value: Double): Double {
        return BigDecimal(value.toString())
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Calculates verse score from 13 category scores (0-10 each).
     */
    fun calculateVerseScore(
        scores: Map<String, Int>,
        explanations: Map<String, String> = emptyMap(),
        isFirstUserVerse: Boolean = false
    ): Pair<Double, List<CategoryScore>> {
        var total = 0.0
        val categoryBreakdown = mutableListOf<CategoryScore>()

        for (cat in CATEGORIES) {
            val displayName = if (isFirstUserVerse && cat.name == "Rebuttal quality") {
                "Response Quality"
            } else {
                cat.name
            }

            val rawScore = (scores[cat.name] ?: scores[displayName] ?: 5).coerceIn(0, 10)
            val contribution = roundToTwoDecimals((rawScore.toDouble() / 10.0) * cat.weight)
            total += contribution

            categoryBreakdown.add(
                CategoryScore(
                    category = displayName,
                    score = rawScore,
                    weight = cat.weight,
                    weightedContribution = contribution,
                    explanation = explanations[cat.name] ?: explanations[displayName] ?: "Score $rawScore/10 based on battle execution."
                )
            )
        }

        return Pair(roundToTwoDecimals(total), categoryBreakdown)
    }

    /**
     * Determines round winner using strict 2-decimal comparison.
     */
    fun determineRoundWinner(userScore: Double, aiScore: Double): Competitor? {
        val diff = roundToTwoDecimals(userScore - aiScore)
        return when {
            diff >= 0.01 -> Competitor.User
            diff <= -0.01 -> Competitor.AI
            else -> null // Draw
        }
    }

    /**
     * Calculates battle final weighted scores.
     * Non-final rounds: weight 1.0. Final round: weight 1.5.
     */
    fun calculateBattleFinalScores(
        rounds: List<Round>,
        totalConfiguredRounds: Int
    ): Triple<Double, Double, Competitor?> {
        if (rounds.isEmpty()) return Triple(0.0, 0.0, null)

        var totalUserWeighted = 0.0
        var totalAiWeighted = 0.0
        var totalWeight = 0.0

        for (r in rounds) {
            val weight = if (r.roundNumber == totalConfiguredRounds) 1.5 else 1.0
            totalUserWeighted += r.userScore * weight
            totalAiWeighted += r.aiScore * weight
            totalWeight += weight
        }

        val finalUser = roundToTwoDecimals(totalUserWeighted / totalWeight)
        val finalAi = roundToTwoDecimals(totalAiWeighted / totalWeight)

        // Compare using 2 decimal places
        val diff = roundToTwoDecimals(finalUser - finalAi)
        val winner = when {
            diff >= 0.01 -> Competitor.User
            diff <= -0.01 -> Competitor.AI
            else -> {
                // Rule 43: Final Tiebreaker -> Compare final round score
                val finalRound = rounds.firstOrNull { it.roundNumber == totalConfiguredRounds }
                if (finalRound != null) {
                    val finalRoundDiff = roundToTwoDecimals(finalRound.userScore - finalRound.aiScore)
                    when {
                        finalRoundDiff >= 0.01 -> Competitor.User
                        finalRoundDiff <= -0.01 -> Competitor.AI
                        else -> null // Confirmed Draw
                    }
                } else {
                    null
                }
            }
        }

        return Triple(finalUser, finalAi, winner)
    }

    /**
     * Returns zero score for a timed out turn.
     */
    fun createTimeoutJudgment(battleId: String, roundNumber: Int, verseId: String): Judgment {
        val categoryScores = CATEGORIES.map { cat ->
            CategoryScore(
                category = cat.name,
                score = 0,
                weight = cat.weight,
                weightedContribution = 0.0,
                explanation = "Turn timed out before submission."
            )
        }
        return Judgment(
            battleId = battleId,
            roundNumber = roundNumber,
            verseId = verseId,
            competitor = Competitor.User,
            normalizedScore = 0.0,
            rationale = "User turn timed out after 90 seconds. Authoritative zero score applied.",
            strengths = emptyList(),
            weaknesses = listOf("Turn expired", "No verse submitted"),
            categoryScores = categoryScores
        )
    }
}
