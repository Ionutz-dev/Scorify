package com.example.scorifynative

import android.content.Context
import android.util.Log
import com.example.scorifynative.network.*
import com.example.scorifynative.sync.OperationType
import com.example.scorifynative.sync.SyncManager
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Repository for server communication
 * Handles all REST API calls and WebSocket communication
 */
class ServerRepository(context: Context) {

    companion object {
        private const val TAG = "ServerRepository"
        private const val BASE_URL = "http://10.0.2.2:3000/" // Android emulator localhost
        // For physical device, use: "http://YOUR_COMPUTER_IP:3000/"

        @Volatile
        private var INSTANCE: ServerRepository? = null

        fun getInstance(context: Context): ServerRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ServerRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val networkMonitor = NetworkMonitor.getInstance(context)
    private val syncManager = SyncManager.getInstance(context)
    private val gson = Gson()

    // Coroutine scope for this repository
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // WebSocket for real-time updates
    private val webSocketManager = WebSocketManager(BASE_URL)

    // Flow for server changes (from WebSocket)
    private val _serverChanges = MutableSharedFlow<WebSocketMessage>(replay = 0)
    val serverChanges: SharedFlow<WebSocketMessage> = _serverChanges.asSharedFlow()

    // Retrofit API service
    private val apiService: ApiService

    init {
        // Setup OkHttp with logging
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("$TAG-HTTP", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        // Setup Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        // Start WebSocket connection
        if (networkMonitor.isCurrentlyConnected()) {
            connectWebSocket()
        }
    }

    /**
     * Connect to WebSocket for real-time updates
     */
    fun connectWebSocket() {
        try {
            webSocketManager.connect()

            // Collect WebSocket messages and forward them
            scope.launch {
                webSocketManager.messageFlow.collect { message ->
                    Log.d(TAG, "WebSocket message received: ${message.type}")
                    _serverChanges.emit(message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect WebSocket", e)
        }
    }

    /**
     * Queue a create operation for later sync (called when adding game offline)
     */
    fun queueCreateForSync(game: Game) {
        Log.d(TAG, "Queueing game ${game.id} for sync")
        syncManager.queueCreateOperation(game)
    }

    /**
     * Queue an update operation for later sync (called when updating game offline)
     */
    fun queueUpdateForSync(game: Game, serverId: Int) {
        Log.d(TAG, "Queueing update for game ${game.id} with server ID $serverId")
        syncManager.queueUpdateOperation(game, serverId)
    }

    /**
     * Queue a delete operation for later sync (called when deleting game offline)
     */
    fun queueDeleteForSync(serverId: Int) {
        Log.d(TAG, "Queueing delete for server game ID $serverId")
        syncManager.queueDeleteOperation(serverId)
    }

    /**
     * Disconnect WebSocket
     */
    fun disconnectWebSocket() {
        webSocketManager.disconnect()
    }

    /**
     * Fetch all games from server (called once at app start)
     */
    suspend fun fetchAllGames(): Result<List<Game>> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) {
            Log.d(TAG, "No network connection, cannot fetch games")
            return@withContext Result.failure(Exception("No internet connection"))
        }

        try {
            Log.d(TAG, "Fetching all games from server...")
            val response = apiService.getAllGames()

            if (response.success && response.data != null) {
                val games = response.data.map { dto -> dtoToGame(dto) }
                Log.d(TAG, "Successfully fetched ${games.size} games from server")
                Result.success(games)
            } else {
                Log.e(TAG, "Server returned error: ${response.message}")
                Result.failure(Exception(response.message ?: "Failed to fetch games"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching games from server", e)
            Result.failure(e)
        }
    }

    /**
     * Create a new game on the server
     */
    suspend fun createGame(game: Game): Result<Game> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) {
            Log.d(TAG, "No network connection, queueing CREATE operation")
            syncManager.queueCreateOperation(game)
            return@withContext Result.success(game.copy(pendingSync = true, syncOperation = "CREATE"))
        }

        try {
            Log.d(TAG, "Creating game on server: ${game.homeTeam} vs ${game.awayTeam}")
            val dto = gameToDto(game)
            val response = apiService.createGame(dto)

            if (response.success && response.data != null) {
                val serverGame = dtoToGame(response.data)
                Log.d(TAG, "Game created successfully with server ID: ${serverGame.serverId}")
                Result.success(serverGame)
            } else {
                Log.e(TAG, "Server returned error: ${response.message}")
                // Queue for later sync
                syncManager.queueCreateOperation(game)
                Result.failure(Exception(response.message ?: "Failed to create game"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating game on server", e)
            // Queue for later sync
            syncManager.queueCreateOperation(game)
            Result.failure(e)
        }
    }

    /**
     * Update a game on the server
     */
    suspend fun updateGame(game: Game): Result<Game> = withContext(Dispatchers.IO) {
        val serverId = game.serverId
        if (serverId == null) {
            Log.e(TAG, "Cannot update game without server ID")
            return@withContext Result.failure(Exception("Game not synced with server"))
        }

        if (!networkMonitor.isCurrentlyConnected()) {
            Log.d(TAG, "No network connection, queueing UPDATE operation")
            syncManager.queueUpdateOperation(game, serverId)
            return@withContext Result.success(game.copy(pendingSync = true, syncOperation = "UPDATE"))
        }

        try {
            Log.d(TAG, "Updating game on server with ID: $serverId")
            val dto = gameToDto(game)
            val response = apiService.updateGame(serverId, dto)

            if (response.success && response.data != null) {
                val serverGame = dtoToGame(response.data)
                Log.d(TAG, "Game updated successfully")
                Result.success(serverGame)
            } else {
                Log.e(TAG, "Server returned error: ${response.message}")
                // Queue for later sync
                syncManager.queueUpdateOperation(game, serverId)
                Result.failure(Exception(response.message ?: "Failed to update game"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating game on server", e)
            // Queue for later sync
            syncManager.queueUpdateOperation(game, serverId)
            Result.failure(e)
        }
    }

    /**
     * Delete a game on the server
     */
    suspend fun deleteGame(serverId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) {
            Log.d(TAG, "No network connection, queueing DELETE operation")
            syncManager.queueDeleteOperation(serverId)
            return@withContext Result.success(true)
        }

        try {
            Log.d(TAG, "Deleting game on server with ID: $serverId")
            val response = apiService.deleteGame(serverId)

            if (response.success) {
                Log.d(TAG, "Game deleted successfully")
                Result.success(true)
            } else {
                Log.e(TAG, "Server returned error: ${response.message}")
                // Queue for later sync
                syncManager.queueDeleteOperation(serverId)
                Result.failure(Exception(response.message ?: "Failed to delete game"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting game on server", e)
            // Queue for later sync
            syncManager.queueDeleteOperation(serverId)
            Result.failure(e)
        }
    }

    /**
     * Process all pending sync operations
     */
    suspend fun processPendingOperations(repository: GameRepository) = withContext(Dispatchers.IO) {
        val operations = syncManager.getPendingOperations()

        if (operations.isEmpty()) {
            Log.d(TAG, "No pending operations to process")
            return@withContext
        }

        Log.d(TAG, "Processing ${operations.size} pending operations")

        for (operation in operations) {
            try {
                when (operation.operationType) {
                    OperationType.CREATE -> {
                        val game = gson.fromJson(operation.gameData, Game::class.java)

                        // Check if this game already has a server ID (might have been created already)
                        val currentGame = repository.getGameById(operation.gameLocalId).getOrNull()

                        if (currentGame?.serverId != null) {
                            // Game already synced, skip CREATE
                            Log.d(TAG, "Skipping CREATE - game ${operation.gameLocalId} already has server ID ${currentGame.serverId}")
                            syncManager.removeOperation(operation.id)
                            continue
                        }

                        val result = createGame(game)

                        if (result.isSuccess) {
                            result.getOrNull()?.let { serverGame ->
                                // Update local game with server ID
                                repository.updateGameServerId(operation.gameLocalId, serverGame.serverId!!)
                                Log.d(TAG, "CREATE synced: local ID ${operation.gameLocalId} -> server ID ${serverGame.serverId}")
                            }
                            syncManager.removeOperation(operation.id)
                        }
                    }

                    OperationType.UPDATE -> {
                        val game = gson.fromJson(operation.gameData, Game::class.java)

                        // Get the current game from database to ensure we have latest data
                        val currentGame = repository.getGameById(game.id).getOrNull()

                        if (currentGame == null) {
                            Log.e(TAG, "Cannot UPDATE - game ${game.id} not found locally")
                            syncManager.removeOperation(operation.id)
                            continue
                        }

                        if (currentGame.serverId == null) {
                            // Game not synced yet, skip this UPDATE (CREATE should happen first)
                            Log.d(TAG, "Skipping UPDATE - game ${game.id} has no server ID yet")
                            continue
                        }

                        // Use the current game data (which includes any local changes)
                        val result = updateGame(currentGame)

                        if (result.isSuccess) {
                            Log.d(TAG, "UPDATE synced: game ${currentGame.id} (server ID ${currentGame.serverId})")
                            syncManager.removeOperation(operation.id)
                        }
                    }

                    OperationType.DELETE -> {
                        val result = deleteGame(operation.gameLocalId)

                        if (result.isSuccess) {
                            Log.d(TAG, "DELETE synced: server ID ${operation.gameLocalId}")
                            syncManager.removeOperation(operation.id)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing operation ${operation.id}: ${e.message}", e)
            }
        }

        Log.d(TAG, "Finished processing pending operations. ${syncManager.getPendingCount()} remaining")
    }

    /**
     * Convert GameDto to Game model
     */
    private fun dtoToGame(dto: GameDto): Game {
        return Game(
            id = 0, // Local ID will be assigned by local database
            homeTeam = dto.homeTeam,
            awayTeam = dto.awayTeam,
            homeScore = dto.homeScore,
            awayScore = dto.awayScore,
            date = Date(dto.date),
            location = dto.location,
            sportType = dto.sportType,
            status = dto.status,
            notes = dto.notes,
            serverId = dto.id,
            pendingSync = false,
            syncOperation = null
        )
    }

    /**
     * Convert Game model to GameDto
     */
    private fun gameToDto(game: Game): GameDto {
        return GameDto(
            id = game.serverId,
            homeTeam = game.homeTeam,
            awayTeam = game.awayTeam,
            homeScore = game.homeScore,
            awayScore = game.awayScore,
            date = game.date.time,
            location = game.location,
            sportType = game.sportType,
            status = game.status,
            notes = game.notes
        )
    }

    /**
     * Check if currently connected to network
     */
    fun isConnected(): Boolean {
        return networkMonitor.isCurrentlyConnected()
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        webSocketManager.cleanup()
        syncManager.cleanup()
        scope.cancel()
    }
}