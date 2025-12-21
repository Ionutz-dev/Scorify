package com.example.scorifynative.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class GameDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        // Database version - increment when schema changes
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "Scorify.db"

        private const val TAG = "GameDbHelper"

        // Singleton instance
        @Volatile
        private var INSTANCE: GameDbHelper? = null

        fun getInstance(context: Context): GameDbHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GameDbHelper(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d(TAG, "Creating database...")
        try {
            db.execSQL(GameContract.SQL_CREATE_ENTRIES)
            Log.d(TAG, "Database created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating database", e)
            throw e
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is a cache for game data
        // For now, we'll just drop and recreate
        Log.d(TAG, "Upgrading database from version $oldVersion to $newVersion")
        try {
            db.execSQL(GameContract.SQL_DELETE_ENTRIES)
            onCreate(db)
            Log.d(TAG, "Database upgraded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error upgrading database", e)
            throw e
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle downgrade the same way as upgrade
        Log.d(TAG, "Downgrading database from version $oldVersion to $newVersion")
        onUpgrade(db, oldVersion, newVersion)
    }

    override fun close() {
        Log.d(TAG, "Closing database...")
        super.close()
        synchronized(GameDbHelper::class.java) {
            INSTANCE = null
        }
    }
}