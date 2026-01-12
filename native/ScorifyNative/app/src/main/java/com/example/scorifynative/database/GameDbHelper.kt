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
        // Database version - INCREMENT when schema changes
        const val DATABASE_VERSION = 2  // Changed from 1 to 2 for server sync support
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
        Log.d(TAG, "Creating database version $DATABASE_VERSION...")
        try {
            db.execSQL(GameContract.SQL_CREATE_ENTRIES)
            Log.d(TAG, "Database created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating database", e)
            throw e
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "Upgrading database from version $oldVersion to $newVersion")

        try {
            if (oldVersion < 2) {
                // Upgrade from version 1 to 2: Add server sync columns
                Log.d(TAG, "Migrating from version 1 to 2: Adding sync columns")

                db.execSQL(GameContract.SQL_ALTER_ADD_SERVER_ID)
                db.execSQL(GameContract.SQL_ALTER_ADD_PENDING_SYNC)
                db.execSQL(GameContract.SQL_ALTER_ADD_SYNC_OPERATION)

                Log.d(TAG, "Migration to version 2 completed successfully")
            }

            // Future migrations can be added here
            // if (oldVersion < 3) { ... }

            Log.d(TAG, "Database upgraded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error upgrading database", e)

            // If migration fails, drop and recreate (not ideal for production)
            Log.w(TAG, "Migration failed, dropping and recreating database")
            db.execSQL(GameContract.SQL_DELETE_ENTRIES)
            onCreate(db)
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle downgrade the same way as upgrade
        Log.d(TAG, "Downgrading database from version $oldVersion to $newVersion")

        // For downgrade, we'll drop and recreate
        Log.w(TAG, "Downgrade detected, dropping and recreating database")
        db.execSQL(GameContract.SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    override fun close() {
        Log.d(TAG, "Closing database...")
        super.close()
        synchronized(GameDbHelper::class.java) {
            INSTANCE = null
        }
    }
}