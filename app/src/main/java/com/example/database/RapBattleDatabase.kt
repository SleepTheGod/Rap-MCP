package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BattleEntity::class], version = 1, exportSchema = false)
abstract class RapBattleDatabase : RoomDatabase() {
    abstract fun battleDao(): BattleDao

    companion object {
        @Volatile
        private var INSTANCE: RapBattleDatabase? = null

        fun getInstance(context: Context): RapBattleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RapBattleDatabase::class.java,
                    "rap_battle_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
