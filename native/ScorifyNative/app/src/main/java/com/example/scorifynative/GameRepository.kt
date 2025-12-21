package com.example.scorifynative

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.scorifynative.database.GameContract
import com.example.scorifynative.database.GameDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class GameRepository(context: Context) {

    private val dbHelper: GameDbHelper = GameDbHelper.getInstance(context)

    companion object {
        private const val TAG = "GameRepository"

        @Volatile
        private var INSTANCE: GameRepository? = null

        fun getInstance(context: Context): GameRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GameRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    suspend fun getAllGames(): Result<List<Game>> = withContext(Dispatchers.IO) {
        var cursor: Cursor? = null
        try {
            val db: SQLiteDatabase = dbHelper.readableDatabase

            val projection = arrayOf(
                GameContract.GameEntry.COLUMN_ID,
                GameContract.GameEntry.COLUMN_HOME_TEAM,
                GameContract.GameEntry.COLUMN_AWAY_TEAM,
                GameContract.GameEntry.COLUMN_HOME_SCORE,
                GameContract.GameEntry.COLUMN_AWAY_SCORE,
                GameContract.GameEntry.COLUMN_DATE,
                GameContract.GameEntry.COLUMN_LOCATION,
                GameContract.GameEntry.COLUMN_SPORT_TYPE,
                GameContract.GameEntry.COLUMN_STATUS,
                GameContract.GameEntry.COLUMN_NOTES
            )

            val sortOrder = "${GameContract.GameEntry.COLUMN_DATE} DESC"

            cursor = db.query(
                GameContract.GameEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
            )

            val games = mutableListOf<Game>()

            with(cursor) {
                while (moveToNext()) {
                    val game = cursorToGame(this)
                    games.add(game)
                }
            }

            Log.d(TAG, "Retrieved ${games.size} games from database")
            Result.success(games)

        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving games", e)
            Result.failure(e)
        } finally {
            cursor?.close()
        }
    }

    suspend fun getGameById(id: Int): Result<Game?> = withContext(Dispatchers.IO) {
        var cursor: Cursor? = null
        try {
            val db: SQLiteDatabase = dbHelper.readableDatabase

            val projection = arrayOf(
                GameContract.GameEntry.COLUMN_ID,
                GameContract.GameEntry.COLUMN_HOME_TEAM,
                GameContract.GameEntry.COLUMN_AWAY_TEAM,
                GameContract.GameEntry.COLUMN_HOME_SCORE,
                GameContract.GameEntry.COLUMN_AWAY_SCORE,
                GameContract.GameEntry.COLUMN_DATE,
                GameContract.GameEntry.COLUMN_LOCATION,
                GameContract.GameEntry.COLUMN_SPORT_TYPE,
                GameContract.GameEntry.COLUMN_STATUS,
                GameContract.GameEntry.COLUMN_NOTES
            )

            val selection = "${GameContract.GameEntry.COLUMN_ID} = ?"
            val selectionArgs = arrayOf(id.toString())

            cursor = db.query(
                GameContract.GameEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
            )

            val game = if (cursor.moveToFirst()) {
                cursorToGame(cursor)
            } else {
                null
            }

            Log.d(TAG, "Retrieved game with id $id")
            Result.success(game)

        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving game with id $id", e)
            Result.failure(e)
        } finally {
            cursor?.close()
        }
    }

    suspend fun addGame(game: Game): Result<Game> = withContext(Dispatchers.IO) {
        try {
            val db: SQLiteDatabase = dbHelper.writableDatabase

            val values = gameToContentValues(game)

            val newRowId = db.insert(GameContract.GameEntry.TABLE_NAME, null, values)

            if (newRowId == -1L) {
                Log.e(TAG, "Error inserting game - insert returned -1")
                Result.failure(Exception("Failed to insert game"))
            } else {
                val insertedGame = game.copy(id = newRowId.toInt())
                Log.d(TAG, "Inserted game with id $newRowId")
                Result.success(insertedGame)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error adding game", e)
            Result.failure(e)
        }
    }

    suspend fun updateGame(game: Game): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val db: SQLiteDatabase = dbHelper.writableDatabase

            val values = gameToContentValues(game)

            // Which row to update, based on the ID
            val selection = "${GameContract.GameEntry.COLUMN_ID} = ?"
            val selectionArgs = arrayOf(game.id.toString())

            val count = db.update(
                GameContract.GameEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs
            )

            if (count > 0) {
                Log.d(TAG, "Updated game with id ${game.id}")
                Result.success(true)
            } else {
                Log.e(TAG, "Error updating game - no rows affected")
                Result.failure(Exception("Game not found"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating game with id ${game.id}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteGame(id: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val db: SQLiteDatabase = dbHelper.writableDatabase

            val selection = "${GameContract.GameEntry.COLUMN_ID} = ?"
            val selectionArgs = arrayOf(id.toString())

            val deletedRows = db.delete(
                GameContract.GameEntry.TABLE_NAME,
                selection,
                selectionArgs
            )

            if (deletedRows > 0) {
                Log.d(TAG, "Deleted game with id $id")
                Result.success(true)
            } else {
                Log.e(TAG, "Error deleting game - no rows affected")
                Result.failure(Exception("Game not found"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error deleting game with id $id", e)
            Result.failure(e)
        }
    }

    private fun cursorToGame(cursor: Cursor): Game {
        val idIndex = cursor.getColumnIndexOrThrow(GameContract.GameEntry.COLUMN_ID)
        val homeTeamIndex = cursor.getColumnIndexOrThrow(GameContract.GameEntry.COLUMN_HOME_TEAM)
        val awayTeamIndex = cursor.getColumnIndexOrThrow(GameContract.GameEntry.COLUMN_AWAY_TEAM)
        val homeScoreIndex = cursor.getColumnIndexOrThrow(GameContract.GameEntry.COLUMN_HOME_SCORE)
        val awayScoreIndex = cursor.getColumnIndexOrThrow(GameContract.GameEntry.COLUMN_AWAY_SCORE)
        val dateIndex = cursor.getColumnIndexOrThrow(GameContract.GameEntry.COLUMN_DATE)
        val locationIndex = cursor.getColumnIndexOrThrow(GameContract.GameEntry.COLUMN_LOCATION)
        val sportTypeIndex = cursor.getColumnIndexOrThrow(GameContract.GameEntry.COLUMN_SPORT_TYPE)
        val statusIndex = cursor.getColumnIndexOrThrow(GameContract.GameEntry.COLUMN_STATUS)
        val notesIndex = cursor.getColumnIndexOrThrow(GameContract.GameEntry.COLUMN_NOTES)

        return Game(
            id = cursor.getInt(idIndex),
            homeTeam = cursor.getString(homeTeamIndex),
            awayTeam = cursor.getString(awayTeamIndex),
            homeScore = cursor.getInt(homeScoreIndex),
            awayScore = cursor.getInt(awayScoreIndex),
            date = Date(cursor.getLong(dateIndex)),
            location = cursor.getString(locationIndex),
            sportType = cursor.getString(sportTypeIndex),
            status = cursor.getString(statusIndex),
            notes = cursor.getString(notesIndex) ?: ""
        )
    }

    private fun gameToContentValues(game: Game): ContentValues {
        return ContentValues().apply {
            put(GameContract.GameEntry.COLUMN_HOME_TEAM, game.homeTeam)
            put(GameContract.GameEntry.COLUMN_AWAY_TEAM, game.awayTeam)
            put(GameContract.GameEntry.COLUMN_HOME_SCORE, game.homeScore)
            put(GameContract.GameEntry.COLUMN_AWAY_SCORE, game.awayScore)
            put(GameContract.GameEntry.COLUMN_DATE, game.date.time)
            put(GameContract.GameEntry.COLUMN_LOCATION, game.location)
            put(GameContract.GameEntry.COLUMN_SPORT_TYPE, game.sportType)
            put(GameContract.GameEntry.COLUMN_STATUS, game.status)
            put(GameContract.GameEntry.COLUMN_NOTES, game.notes)
        }
    }

    fun close() {
        dbHelper.close()
    }
}