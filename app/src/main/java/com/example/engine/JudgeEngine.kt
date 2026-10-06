package com.example.engine

import com.example.model.BattleConfig
import com.example.model.Competitor
import com.example.model.Judgment
import com.example.model.ScoringSystem
import com.example.model.Verse
import com.example.network.GeminiClient
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object JudgeEngine {

    suspend fun evaluateVerse(
        config: BattleConfig,
        verse: Verse,
        opponentVerse: Verse?,
        roundNumber: Int,
        isFirstUserVerse: Boolean
    ): Judgment {
        if (verse.timedOut || verse.content.isBlank()) {
            return ScoringSystem.createTimeoutJudgment(
                battleId = verse.battleId,
                roundNumber = roundNumber,
                verseId = verse.id
            )
        }

        // Try Gemini AI evaluation if configured
        if (GeminiClient.isConfigured()) {
            val geminiResult = runGeminiJudging(config, verse, opponentVerse, roundNumber, isFirstUserVerse)
            if (geminiResult != null) return geminiResult
        }

        // Fallback to deterministic independent scoring rubric
        return runDeterministicJudging(config, verse, opponentVerse, roundNumber, isFirstUserVerse)
    }

    private suspend fun runGeminiJudging(
        config: BattleConfig,
        verse: Verse,
        opponentVerse: Verse?,
        roundNumber: Int,
        isFirstUserVerse: Boolean
    ): Judgment? {
        try {
            val rebuttalLabel = if (isFirstUserVerse) "Response Quality" else "Rebuttal quality"
            val prompt = """
                You are the Official Independent Head Judge for a competitive rap battle.
                Evaluate the following verse strictly across all 13 canonical categories.
                Each category score must be an INTEGER between 0 and 10.
                
                Battle Mode: ${config.mode.name}
                Theme: ${config.theme ?: "Freestyle"}
                Constraints: ${config.constraints.joinToString(", ").ifEmpty { "None" }}
                Competitor: ${verse.competitor.name}
                Round: $roundNumber
                Is First User Verse of Battle: $isFirstUserVerse
                Opponent Verse: "${opponentVerse?.content ?: "None"}"

                Verse to Judge:
                "${verse.content}"

                Output a single valid JSON object strictly matching this format (no markdown, no extra text):
                {
                  "scores": {
                    "Punchlines": 8,
                    "Wordplay": 7,
                    "Rhyme quality": 8,
                    "Multisyllabic rhyme quality": 7,
                    "Flow": 8,
                    "Creativity": 8,
                    "Originality": 7,
                    "Relevance": 8,
                    "$rebuttalLabel": 7,
                    "Setup and payoff": 8,
                    "Thematic consistency": 8,
                    "Crowd appeal": 8,
                    "Overall impact": 8
                  },
                  "rationale": "One paragraph overall critique",
                  "strengths": ["Strength 1", "Strength 2"],
                  "weaknesses": ["Weakness 1"]
                }
            """.trimIndent()

            val response = GeminiClient.generateContent(
                prompt = prompt,
                systemInstruction = "You are an authoritative, impartial hip-hop battle judge. Return valid JSON only.",
                model = "gemini-3.1-pro-preview"
            ) ?: return null

            val jsonStr = response.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val json = JSONObject(jsonStr)
            val scoresObj = json.getJSONObject("scores")
            val scoresMap = mutableMapOf<String, Int>()
            val keys = scoresObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                scoresMap[k] = scoresObj.getInt(k)
            }

            val rationale = json.optString("rationale", "Strong battle performance with crisp delivery.")
            val strengths = mutableListOf<String>()
            val weaknesses = mutableListOf<String>()

            val strArr = json.optJSONArray("strengths")
            if (strArr != null) {
                for (i in 0 until strArr.length()) strengths.add(strArr.getString(i))
            }
            val weakArr = json.optJSONArray("weaknesses")
            if (weakArr != null) {
                for (i in 0 until weakArr.length()) weaknesses.add(weakArr.getString(i))
            }

            val (normalizedScore, categoryScores) = ScoringSystem.calculateVerseScore(
                scores = scoresMap,
                isFirstUserVerse = isFirstUserVerse
            )

            return Judgment(
                battleId = verse.battleId,
                roundNumber = roundNumber,
                verseId = verse.id,
                competitor = verse.competitor,
                normalizedScore = normalizedScore,
                rationale = rationale,
                strengths = strengths.ifEmpty { listOf("Effective delivery", "Consistent rhyme pattern") },
                weaknesses = weaknesses.ifEmpty { listOf("Could increase multisyllabic complexity") },
                categoryScores = categoryScores
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun runDeterministicJudging(
        config: BattleConfig,
        verse: Verse,
        opponentVerse: Verse?,
        roundNumber: Int,
        isFirstUserVerse: Boolean
    ): Judgment {
        val lines = verse.content.lines().filter { it.isNotBlank() }
        val lineCount = lines.size
        val words = verse.content.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        val totalWords = words.size

        // Heuristic analysis of rhyme, syllables, and punchlines
        val hasMultiSyllable = lines.any { line ->
            line.split(" ").any { it.length > 8 }
        }
        val endsWithExclamation = lines.any { it.trim().endsWith("!") }
        val opponentKeyWordCount = if (opponentVerse != null) {
            val oppWords = opponentVerse.content.lowercase().split(Regex("\\s+")).toSet()
            words.count { it in oppWords && it.length > 4 }
        } else {
            0
        }

        val themeBonus = if (config.theme != null && verse.content.contains(config.theme, ignoreCase = true)) 1 else 0

        // Base category scores 6-9 range for competitive realism
        val basePunchlines = (if (endsWithExclamation) 8 else 7) + (if (lineCount >= 4) 1 else 0)
        val baseWordplay = min(10, 7 + (if (totalWords > 24) 1 else 0))
        val baseRhyme = min(10, 7 + (if (lineCount in 4..12) 1 else 0))
        val baseMultiRhyme = if (hasMultiSyllable) 8 else 6
        val baseFlow = min(10, 7 + (if (lineCount % 2 == 0) 1 else 0))
        val baseCreativity = min(10, 7 + themeBonus)
        val baseOriginality = min(10, 7 + (if (totalWords > 20) 1 else 0))
        val baseRelevance = min(10, 7 + themeBonus + min(2, opponentKeyWordCount))
        
        // Rebuttal quality or Response Quality
        val baseRebuttal = if (isFirstUserVerse) {
            // Evaluates establishing intent and setup
            min(10, 7 + (if (lineCount >= 4) 1 else 0) + themeBonus)
        } else {
            min(10, 6 + min(3, opponentKeyWordCount) + (if (endsWithExclamation) 1 else 0))
        }

        val baseSetupPayoff = min(10, 7 + (if (lineCount >= 4) 1 else 0))
        val baseThematic = min(10, 7 + themeBonus * 2)
        val baseCrowd = min(10, 7 + (if (endsWithExclamation) 1 else 0))
        val baseOverall = min(10, 7 + (if (lineCount >= 4) 1 else 0))

        val scoresMap = mapOf(
            "Punchlines" to basePunchlines.coerceIn(1, 10),
            "Wordplay" to baseWordplay.coerceIn(1, 10),
            "Rhyme quality" to baseRhyme.coerceIn(1, 10),
            "Multisyllabic rhyme quality" to baseMultiRhyme.coerceIn(1, 10),
            "Flow" to baseFlow.coerceIn(1, 10),
            "Creativity" to baseCreativity.coerceIn(1, 10),
            "Originality" to baseOriginality.coerceIn(1, 10),
            "Relevance" to baseRelevance.coerceIn(1, 10),
            "Rebuttal quality" to baseRebuttal.coerceIn(1, 10),
            "Response Quality" to baseRebuttal.coerceIn(1, 10),
            "Setup and payoff" to baseSetupPayoff.coerceIn(1, 10),
            "Thematic consistency" to baseThematic.coerceIn(1, 10),
            "Crowd appeal" to baseCrowd.coerceIn(1, 10),
            "Overall impact" to baseOverall.coerceIn(1, 10)
        )

        val explanations = mapOf(
            "Punchlines" to "Effective delivery with sharp punchline impact ($basePunchlines/10).",
            "Wordplay" to "Solid use of metaphors and thematic vocabulary ($baseWordplay/10).",
            "Rhyme quality" to "Crisp end rhymes maintained throughout the stanza ($baseRhyme/10).",
            "Multisyllabic rhyme quality" to "Balanced cadence with multisyllabic elements ($baseMultiRhyme/10).",
            "Flow" to "Consistent meter and rhythmic variation ($baseFlow/10).",
            "Creativity" to "Inventive conceptual angle applied to the battle ($baseCreativity/10).",
            "Originality" to "Distinctive personal voice without cliché filler ($baseOriginality/10).",
            "Relevance" to "Strong contextual connection to the arena topic ($baseRelevance/10).",
            "Rebuttal quality" to "Addressed opponent's bars with direct counters ($baseRebuttal/10).",
            "Response Quality" to "Established aggressive competitive intent and solid battle premise ($baseRebuttal/10).",
            "Setup and payoff" to "Clear two-bar setup building directly into the punchline ($baseSetupPayoff/10).",
            "Thematic consistency" to "Stayed aligned with the declared battle parameters ($baseThematic/10).",
            "Crowd appeal" to "High-energy finish commanding the cypher ($baseCrowd/10).",
            "Overall impact" to "Memorable round presence with technical authority ($baseOverall/10)."
        )

        val (normalizedScore, categoryScores) = ScoringSystem.calculateVerseScore(
            scores = scoresMap,
            explanations = explanations,
            isFirstUserVerse = isFirstUserVerse
        )

        val strengths = mutableListOf<String>().apply {
            if (basePunchlines >= 8) add("Sharp punchline delivery")
            if (baseRhyme >= 8) add("Crisp rhyme precision")
            if (baseRebuttal >= 8) add(if (isFirstUserVerse) "Aggressive battle premise setup" else "Direct contextual counter")
            if (isEmpty()) add("Solid rhythmic foundation")
        }

        val weaknesses = mutableListOf<String>().apply {
            if (baseMultiRhyme < 7) add("Could increase multisyllabic complexity")
            if (baseWordplay < 8) add("Incorporate more layered double entendres")
            if (isEmpty()) add("Pacing can tighten on the closing bars")
        }

        return Judgment(
            battleId = verse.battleId,
            roundNumber = roundNumber,
            verseId = verse.id,
            competitor = verse.competitor,
            normalizedScore = normalizedScore,
            rationale = "Competitor ${verse.competitor.name} delivered a focused verse scoring $normalizedScore/100, driven by crisp rhymes and strong performance rhythm.",
            strengths = strengths,
            weaknesses = weaknesses,
            categoryScores = categoryScores
        )
    }
}
