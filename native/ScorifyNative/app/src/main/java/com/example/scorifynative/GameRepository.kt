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
                GameContract.GameEntry.COLUMN_NOTES,
                GameContract.GameEntry.COLUMN_SERVER_ID,
                GameContract.GameEntry.COLUMN_PENDING_SYNC,
                GameContract.GameEntry.COLUMN_SYNC_OPERATION
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

            Log.d(TAG, "Retrieved ${games.size} games from local database")
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
                GameContract.GameEntry.COLUMN_NOTES,
                GameContract.GameEntry.COLUMN_SERVER_ID,
                GameContract.GameEntry.COLUMN_PENDING_SYNC,
                GameContract.GameEntry.COLUMN_SYNC_OPERATION
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
                Log.d(TAG, "Inserted game with local id $newRowId")
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

            val selection = "${GameContract.GameEntry.COLUMN_ID} = ?"
            val selectionArgs = arrayOf(game.id.toString())

            val count = db.update(
                GameContract.GameEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs
            )

            if (count > 0) {
                Log.d(TAG, "Updated game with local id ${game.id}")
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
                Log.d(TAG, "Deleted game with local id $id")
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

    /**
     * Update the server ID for a game after successful sync
     */
    suspend fun updateGameServerId(localId: Int, serverId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val db: SQLiteDatabase = dbHelper.writableDatabase

            val values = ContentValues().apply {
                put(GameContract.GameEntry.COLUMN_SERVER_ID, serverId)
                put(GameContract.GameEntry.COLUMN_PENDING_SYNC, 0)
                put(GameContract.GameEntry.COLUMN_SYNC_OPERATION, null as String?)
            }

            val selection = "${GameContract.GameEntry.COLUMN_ID} = ?"
            val selectionArgs = arrayOf(localId.toString())

            val count = db.update(
                GameContract.GameEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs
            )

            if (count > 0) {
                Log.d(TAG, "Updated game $localId with server ID $serverId")
                Result.success(true)
            } else {
                Log.e(TAG, "Failed to update server ID - game not found")
                Result.failure(Exception("Game not found"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating server ID", e)
            Result.failure(e)
        }
    }

    /**
     * Find game by server ID
     */
    suspend fun getGameByServerId(serverId: Int): Result<Game?> = withContext(Dispatchers.IO) {
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
                GameContract.GameEntry.COLUMN_NOTES,
                GameContract.GameEntry.COLUMN_SERVER_ID,
                GameContract.GameEntry.COLUMN_PENDING_SYNC,
                GameContract.GameEntry.COLUMN_SYNC_OPERATION
            )

            val selection = "${GameContract.GameEntry.COLUMN_SERVER_ID} = ?"
            val selectionArgs = arrayOf(serverId.toString())

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

            Result.success(game)

        } catch (e: Exception) {
            Log.e(TAG, "Error finding game by server ID $serverId", e)
            Result.failure(e)
        } finally {
            cursor?.close()
        }
    }

    /**
     * Save or update games from server (used during initial fetch)
     */
    suspend fun saveGamesFromServer(serverGames: List<Game>): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val db: SQLiteDatabase = dbHelper.writableDatabase

            db.beginTransaction()
            try {
                for (serverGame in serverGames) {
                    // Check if game already exists by server ID
                    val existingGame = getGameByServerId(serverGame.serverId!!).getOrNull()

                    if (existingGame != null) {
                        // Update existing game - preserve local ID, update with server data
                        val updatedGame = serverGame.copy(
                            id = existingGame.id,  // Keep the local ID
                            pendingSync = false,
                            syncOperation = null
                        )
                        updateGame(updatedGame)
                        Log.d(TAG, "Updated existing game ${existingGame.id} with server data")
                    } else {
                        // Insert new game from server
                        addGame(serverGame)
                        Log.d(TAG, "Inserted new game from server with server ID ${serverGame.serverId}")
                    }
                }

                db.setTransactionSuccessful()
                Log.d(TAG, "Saved ${serverGames.size} games from server")
                Result.success(true)

            } finally {
                db.endTransaction()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error saving games from server", e)
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
        val serverIdIndex = cursor.getColumnIndexOrThrow(GameContract.GameEntry.COLUMN_SERVER_ID)
        val pendingSyncIndex = cursor.getColumnIndexOrThrow(GameContract.GameEntry.COLUMN_PENDING_SYNC)
        val syncOperationIndex = cursor.getColumnIndexOrThrow(GameContract.GameEntry.COLUMN_SYNC_OPERATION)

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
            notes = cursor.getString(notesIndex) ?: "",
            serverId = if (cursor.isNull(serverIdIndex)) null else cursor.getInt(serverIdIndex),
            pendingSync = cursor.getInt(pendingSyncIndex) == 1,
            syncOperation = cursor.getString(syncOperationIndex)
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
            put(GameContract.GameEntry.COLUMN_SERVER_ID, game.serverId)
            put(GameContract.GameEntry.COLUMN_PENDING_SYNC, if (game.pendingSync) 1 else 0)
            put(GameContract.GameEntry.COLUMN_SYNC_OPERATION, game.syncOperation)
        }
    }

    fun close() {
        dbHelper.close()
    }
}