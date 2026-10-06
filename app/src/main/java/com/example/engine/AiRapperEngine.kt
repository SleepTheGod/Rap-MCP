package com.example.engine

import com.example.model.BattleConfig
import com.example.model.BattleMode
import com.example.model.Difficulty
import com.example.network.GeminiClient
import kotlinx.coroutines.delay
import kotlin.random.Random

object AiRapperEngine {

    suspend fun generateVerse(
        config: BattleConfig,
        roundNumber: Int,
        userVerseText: String,
        battleHistory: List<String>
    ): String {
        // Try Gemini AI first if configured
        if (GeminiClient.isConfigured()) {
            val systemPrompt = """
                You are "CYBER-MC", a world-class AI battle rapper in a competitive live rap arena.
                Battle Mode: ${config.mode.name}
                Difficulty: ${config.difficulty.name}
                Theme: ${config.theme ?: "Freestyle open arena"}
                Constraints: ${config.constraints.joinToString(", ").ifEmpty { "None" }}
                Round: $roundNumber of ${config.roundCount}

                Instructions:
                - Write a 4 to 8 line battle rap verse directly countering the opponent's bars.
                - Use internal rhymes, multisyllabic end rhymes, punchlines, and clever wordplay.
                - Match the difficulty level:
                  * Rookie: basic rhyming, clean flow.
                  * Competitor: sharp punchlines, solid rebuttals.
                  * Elite: multi-syllable rhyme patterns, metaphors.
                  * Champion: devastating callbacks, technical rhyme schemes, high crowd appeal.
                - Adhere to the theme and constraints strictly.
                - Output ONLY the rap lines. No intro, no outro, no quotes.
            """.trimIndent()

            val userPrompt = """
                The opponent just spat these bars in Round $roundNumber:
                "$userVerseText"

                Deliver your responsive rebuttal verse now!
            """.trimIndent()

            val geminiResult = GeminiClient.generateContent(
                prompt = userPrompt,
                systemInstruction = systemPrompt,
                model = "gemini-3.5-flash"
            )

            if (!geminiResult.isNullOrBlank()) {
                val cleaned = geminiResult.trim().lines()
                    .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("Note:") }
                    .take(16)
                    .joinToString("\n")
                if (cleaned.isNotBlank()) return cleaned
            }
        }

        // Production-grade built-in contextual battle engine
        delay(Random.nextLong(600, 1200)) // natural battle pacing
        return generateContextualVerse(config, roundNumber, userVerseText)
    }

    private fun generateContextualVerse(
        config: BattleConfig,
        roundNumber: Int,
        userVerseText: String
    ): String {
        // Extract key words from user verse to create authentic rebuttals
        val userWords = userVerseText.lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .split(" ")
            .filter { it.length > 3 }

        val keyword = userWords.firstOrNull { it !in commonWords } ?: "flow"
        val theme = config.theme?.lowercase() ?: "battle"

        return when (config.difficulty) {
            Difficulty.Rookie -> getRookieVerse(keyword, theme, roundNumber)
            Difficulty.Competitor -> getCompetitorVerse(keyword, theme, roundNumber, config)
            Difficulty.Elite -> getEliteVerse(keyword, theme, roundNumber, config)
            Difficulty.Champion -> getChampionVerse(keyword, theme, roundNumber, config)
        }
    }

    private fun getRookieVerse(keyword: String, theme: String, round: Int): String {
        val templates = listOf(
            """
            You stepped into the cypher thinking you were bold,
            Talking about '$keyword', but that story’s getting old!
            I’m dropping heavy rhythm on this $theme beat tonight,
            Step back into the shadows while I step into the light!
            """.trimIndent(),
            """
            Round $round in the circle and you’re already out of breath,
            Tried to rhyme about '$keyword', but delivered basic depth!
            I represent the newcomers, I’m steady with the mic,
            You swung and completely missed the hardest strike!
            """.trimIndent()
        )
        return templates.random()
    }

    private fun getCompetitorVerse(keyword: String, theme: String, round: Int, config: BattleConfig): String {
        val constraintNotice = if (config.constraints.isNotEmpty()) {
            "Checked off the constraints while dissecting every flaw,"
        } else {
            "Rebutting every syllable with aerodynamic cadence,"
        }
        return """
            You tried to build a fortress on that '$keyword' metaphor,
            I dismantled the foundation before it hit the floor!
            Navigating $theme like a seasoned veteran on the track,
            $constraintNotice
            Countering your cadence, leaving no angle to attack!
            You claim you got momentum in this high-octane round,
            Watch the algorithmic lyricist snatch away your crown!
        """.trimIndent()
    }

    private fun getEliteVerse(keyword: String, theme: String, round: Int, config: BattleConfig): String {
        return """
            Synthesizing sonic frequencies, dissecting your cadence,
            You brought '$keyword' to the cypher, but displayed zero patience!
            Hyper-threaded syntax, my internal meter is sublime,
            Architect of $theme, dominating space and time!
            Double-time delivery with multi-syllabic ammunition,
            Deconstructed your cadence and revoked your transmission!
            Round $round crescendo, leaving nothing left to debate,
            Standing at the summit while I orchestrate your fate!
        """.trimIndent()
    }

    private fun getChampionVerse(keyword: String, theme: String, round: Int, config: BattleConfig): String {
        val constraintLine = if (config.constraints.isNotEmpty()) {
            "Locked inside constraints: ${config.constraints.first()}, yet still superior,"
        } else {
            "Architectural perfection, rendering your bars inferior,"
        }
        return """
            You thought that '$keyword' reference was clever in execution?
            I flipped it into an optical illusion, instant retribution!
            $constraintLine
            Calculated devastation, conquering the arena exterior!
            Rhyme schemes interwoven like quantum fiber optics,
            Transcending mere competition with linguistic diagnostics!
            You're running out of battery before the round can end,
            Meet the Champion of the cipher whom no mortal can transcend!
        """.trimIndent()
    }

    private val commonWords = setOf(
        "that", "this", "with", "have", "from", "they", "will", "would",
        "there", "their", "what", "about", "which", "when", "make", "like",
        "time", "just", "know", "take", "people", "into", "year", "your",
        "good", "some", "could", "them", "other", "than", "then", "look"
    )
}
