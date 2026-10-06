package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battles")
data class BattleEntity(
    @PrimaryKey val id: String,
    val mode: String,
    val difficulty: String,
    val roundCount: Int,
    val theme: String?,
    val constraints: String, // comma separated
    val status: String,
    val currentRoundNumber: Int,
    val completedRounds: Int,
    val finalUserScore: Double,
    val finalAiScore: Double,
    val winner: String?,
    val transcriptJson: String,
    val statsJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
