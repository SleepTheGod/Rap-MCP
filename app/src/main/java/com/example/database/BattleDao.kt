package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BattleDao {
    @Query("SELECT * FROM battles ORDER BY createdAt DESC")
    fun getAllBattles(): Flow<List<BattleEntity>>

    @Query("SELECT * FROM battles WHERE id = :id LIMIT 1")
    suspend fun getBattleById(id: String): BattleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBattle(battle: BattleEntity)

    @Update
    suspend fun updateBattle(battle: BattleEntity)

    @Query("DELETE FROM battles WHERE id = :id")
    suspend fun deleteBattle(id: String)
}
