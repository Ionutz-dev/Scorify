package com.example.scorifynative

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.scorifynative.network.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository = GameRepository.getInstance(application)
    private val serverRepository: ServerRepository = ServerRepository.getInstance(application)
    private val networkMonitor: NetworkMonitor = NetworkMonitor.getInstance(application)

    // Games list state
    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games: StateFlow<List<Game>> = _games.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Success message state
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Network connectivity state
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Track recently synced server IDs to avoid duplicates from WebSocket echoes
    private val recentlySyncedGameIds = mutableSetOf<Int>()

    private var loadGamesJob: Job? = null

    companion object {
        private const val TAG = "GameViewModel"
    }

    init {
        // Monitor network connectivity
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                _isConnected.value = connected
                Log.d(TAG, "Network connectivity changed: $connected")

                if (connected) {
                    // Try to sync pending operations when connection restored
                    syncPendingOperations()
                }
            }
        }

        // Listen for WebSocket updates from server
        viewModelScope.launch {
            serverRepository.serverChanges.collect { message ->
                Log.d(TAG, "WebSocket update received: ${message.type}")

                when (message.type) {
                    "CREATE" -> {
                        message.data?.let { dto ->
                            // Check if this is an echo of our own create operation
                            if (recentlySyncedGameIds.contains(dto.id)) {
                                Log.d(TAG, "Ignoring WebSocket CREATE echo for game ${dto.id}")
                                recentlySyncedGameIds.remove(dto.id)
                                return@collect
                            }

                            // Add new game from server to local database
                            val game = Game(
                                id = 0,
                                homeTeam = dto.homeTeam,
                                awayTeam = dto.awayTeam,
                                homeScore = dto.homeScore,
                                awayScore = dto.awayScore,
                                date = java.util.Date(dto.date),
                                location = dto.location,
                                sportType = dto.sportType,
                                status = dto.status,
                                notes = dto.notes,
                                serverId = dto.id,
                                pendingSync = false,
                                syncOperation = null
                            )

                            // Check if we already have this game
                            viewModelScope.launch {
                                val existingGame = repository.getGameByServerId(dto.id!!).getOrNull()
                                if (existingGame == null) {
                                    repository.addGame(game)
                                    loadGames()
                                    Log.d(TAG, "Added game from WebSocket: ${dto.homeTeam} vs ${dto.awayTeam}")
                                } else {
                                    Log.d(TAG, "Game ${dto.id} already exists locally, skipping")
                                }
                            }
                        }
                    }

                    "UPDATE" -> {
                        message.data?.let { dto ->
                            // Check if this is an echo of our own update operation
                            if (recentlySyncedGameIds.contains(dto.id)) {
                                Log.d(TAG, "Ignoring WebSocket UPDATE echo for game ${dto.id}")
                                recentlySyncedGameIds.remove(dto.id)
                                return@collect
                            }

                            // Update game from server
                            viewModelScope.launch {
                                val existingGame = repository.getGameByServerId(dto.id!!).getOrNull()
                                if (existingGame != null) {
                                    val updatedGame = existingGame.copy(
                                        homeTeam = dto.homeTeam,
                                        awayTeam = dto.awayTeam,
                                        homeScore = dto.homeScore,
                                        awayScore = dto.awayScore,
                                        date = java.util.Date(dto.date),
                                        location = dto.location,
                                        sportType = dto.sportType,
                                        status = dto.status,
                                        notes = dto.notes
                                    )
                                    repository.updateGame(updatedGame)
                                    loadGames()
                                    Log.d(TAG, "Updated game from WebSocket: ${dto.homeTeam} vs ${dto.awayTeam}")
                                }
                            }
                        }
                    }

                    "DELETE" -> {
                        message.data?.let { dto ->
                            // Check if this is an echo of our own delete operation
                            if (recentlySyncedGameIds.contains(dto.id)) {
                                Log.d(TAG, "Ignoring WebSocket DELETE echo for game ${dto.id}")
                                recentlySyncedGameIds.remove(dto.id)
                                return@collect
                            }

                            // Delete game from server
                            viewModelScope.launch {
                                val existingGame = repository.getGameByServerId(dto.id!!).getOrNull()
                                if (existingGame != null) {
                                    repository.deleteGame(existingGame.id)
                                    loadGames()
                                    Log.d(TAG, "Deleted game from WebSocket: server id ${dto.id}")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Initial load
        loadGames()

        // Fetch from server if connected
        if (networkMonitor.isCurrentlyConnected()) {
            fetchGamesFromServer()

            // Also trigger sync for pending operations on app start
            viewModelScope.launch {
                delay(1500) // Wait for server fetch to complete
                Log.d(TAG, "Triggering initial sync check on app start")
                syncPendingOperations()
            }
        }
    }

    /**
     * Load games from local database
     */
    fun loadGames() {
        // Cancel any pending load operation to prevent race conditions
        loadGamesJob?.cancel()

        loadGamesJob = viewModelScope.launch {
            // Small delay to batch rapid calls
            delay(100)

            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getAllGames()

            result.fold(
                onSuccess = { gamesList ->
                    _games.value = gamesList
                    Log.d(TAG, "Successfully loaded ${gamesList.size} games from local database")
                },
                onFailure = { exception ->
                    val message = "Failed to load games: ${exception.message}"
                    _errorMessage.value = message
                    Log.e(TAG, message, exception)
                }
            )

            _isLoading.value = false
        }
    }

    /**
     * Fetch games from server (called once at app start)
     */
    private fun fetchGamesFromServer() {
        viewModelScope.launch {
            Log.d(TAG, "Fetching games from server...")

            val result = serverRepository.fetchAllGames()

            result.fold(
                onSuccess = { serverGames ->
                    // Save server games to local database
                    repository.saveGamesFromServer(serverGames)
                    // Reload from local database
                    loadGames()
                    Log.d(TAG, "Successfully fetched and saved ${serverGames.size} games from server")
                },
                onFailure = { exception ->
                    Log.e(TAG, "Failed to fetch games from server: ${exception.message}")
                    // Continue with local data only
                }
            )
        }
    }

    /**
     * Add a new game
     */
    fun addGame(game: Game) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            // First, save to local database
            val localResult = repository.addGame(game)

            localResult.fold(
                onSuccess = { insertedGame ->
                    Log.d(TAG, "Game saved locally with id ${insertedGame.id}")

                    // Try to sync with server
                    if (networkMonitor.isCurrentlyConnected()) {
                        val serverResult = serverRepository.createGame(insertedGame)

                        serverResult.fold(
                            onSuccess = { serverGame ->
                                // Track this server ID to ignore WebSocket echo
                                recentlySyncedGameIds.add(serverGame.serverId!!)

                                // Update local game with server ID
                                repository.updateGameServerId(insertedGame.id, serverGame.serverId!!)
                                _successMessage.value = "Game added successfully!"
                                Log.d(TAG, "Game synced with server, server ID: ${serverGame.serverId}")

                                // Remove from tracking after 5 seconds
                                viewModelScope.launch {
                                    delay(5000)
                                    recentlySyncedGameIds.remove(serverGame.serverId)
                                }
                            },
                            onFailure = { exception ->
                                _successMessage.value = "Game saved (will sync when online)"
                                Log.d(TAG, "Game queued for sync: ${exception.message}")
                            }
                        )
                    } else {
                        // Queue with correct local ID for offline sync
                        serverRepository.queueCreateForSync(insertedGame)
                        _successMessage.value = "Game saved (offline mode)"
                        Log.d(TAG, "Game saved in offline mode with id ${insertedGame.id}")
                    }

                    loadGames()
                },
                onFailure = { exception ->
                    val message = "Failed to add game: ${exception.message}"
                    _errorMessage.value = message
                    Log.e(TAG, message, exception)
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * Update an existing game
     */
    fun updateGame(game: Game) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            // First, update local database
            val localResult = repository.updateGame(game)

            localResult.fold(
                onSuccess = {
                    Log.d(TAG, "Game updated locally with id ${game.id}")

                    // Only sync UPDATE if game already has a server ID
                    if (game.serverId != null) {
                        if (networkMonitor.isCurrentlyConnected()) {
                            // Online - update on server immediately
                            val serverResult = serverRepository.updateGame(game)

                            serverResult.fold(
                                onSuccess = {
                                    // Track this server ID to ignore WebSocket echo
                                    recentlySyncedGameIds.add(game.serverId!!)

                                    _successMessage.value = "Game updated successfully!"
                                    Log.d(TAG, "Game synced with server")

                                    // Remove from tracking after 5 seconds
                                    viewModelScope.launch {
                                        delay(5000)
                                        recentlySyncedGameIds.remove(game.serverId)
                                    }
                                },
                                onFailure = { exception ->
                                    _successMessage.value = "Game updated (will sync when online)"
                                    Log.d(TAG, "Game update queued for sync: ${exception.message}")
                                }
                            )
                        } else {
                            // Offline - queue the update operation
                            serverRepository.queueUpdateForSync(game, game.serverId!!)
                            _successMessage.value = "Game updated (offline mode)"
                            Log.d(TAG, "Game updated in offline mode, queued for sync")
                        }
                    } else {
                        // Game not synced yet - user is editing an offline game
                        // The CREATE operation should be in the sync queue from addGame
                        _successMessage.value = "Game updated locally"
                        Log.d(TAG, "Updated offline game, CREATE will sync when triggered")
                    }

                    loadGames()
                },
                onFailure = { exception ->
                    val message = "Failed to update game: ${exception.message}"
                    _errorMessage.value = message
                    Log.e(TAG, message, exception)
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * Delete a game
     */
    fun deleteGame(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            // Get the game to check server ID BEFORE deleting
            val gameResult = repository.getGameById(id)
            val game = gameResult.getOrNull()

            if (game == null) {
                _errorMessage.value = "Game not found"
                _isLoading.value = false
                return@launch
            }

            // Delete from local database
            val localResult = repository.deleteGame(id)

            localResult.fold(
                onSuccess = {
                    Log.d(TAG, "Game deleted locally with id $id")

                    // Only try to delete from server if game has a server ID
                    if (game.serverId != null) {
                        if (networkMonitor.isCurrentlyConnected()) {
                            // Online - delete immediately from server
                            val serverResult = serverRepository.deleteGame(game.serverId!!)

                            serverResult.fold(
                                onSuccess = {
                                    // Track this server ID to ignore WebSocket echo
                                    recentlySyncedGameIds.add(game.serverId!!)

                                    _successMessage.value = "Game deleted successfully!"
                                    Log.d(TAG, "Game deleted from server")

                                    // Remove from tracking after 5 seconds
                                    viewModelScope.launch {
                                        delay(5000)
                                        recentlySyncedGameIds.remove(game.serverId)
                                    }
                                },
                                onFailure = { exception ->
                                    _successMessage.value = "Game deleted (will sync when online)"
                                    Log.d(TAG, "Game deletion queued for sync: ${exception.message}")
                                }
                            )
                        } else {
                            // Offline - queue for deletion
                            serverRepository.queueDeleteForSync(game.serverId!!)
                            _successMessage.value = "Game deleted (will sync when online)"
                            Log.d(TAG, "Game deletion queued for server sync")
                        }
                    } else {
                        // Game never synced to server, just delete locally
                        _successMessage.value = "Game deleted"
                        Log.d(TAG, "Deleted offline game that was never synced to server")
                    }

                    loadGames()
                },
                onFailure = { exception ->
                    val message = "Failed to delete game: ${exception.message}"
                    _errorMessage.value = message
                    Log.e(TAG, message, exception)
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * Sync pending operations with server
     */
    private fun syncPendingOperations() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting sync of pending operations...")
                serverRepository.processPendingOperations(repository)
                loadGames() // Reload to show updated sync status
                Log.d(TAG, "Sync completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing pending operations", e)
            }
        }
    }

    /**
     * Get a game by ID
     */
    fun getGameById(id: Int): Game? {
        return _games.value.find { it.id == id }
    }

    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
        serverRepository.cleanup()
        Log.d(TAG, "ViewModel cleared, resources cleaned up")
    }
}