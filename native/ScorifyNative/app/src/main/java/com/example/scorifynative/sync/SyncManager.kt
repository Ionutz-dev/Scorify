package com.example.scorifynative.sync

import android.content.Context
import android.util.Log
import com.example.scorifynative.Game
import com.example.scorifynative.network.GameDto
import com.example.scorifynative.network.NetworkMonitor
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Manages synchronization of offline operations with the server
 * Queues operations when offline and syncs when connection is restored
 */
class SyncManager(context: Context) {

    companion object {
        private const val TAG = "SyncManager"

        @Volatile
        private var INSTANCE: SyncManager? = null

        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val networkMonitor = NetworkMonitor.getInstance(context)
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Queue for pending operations
    private val pendingQueue = ConcurrentLinkedQueue<PendingOperation>()

    // Sync status
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    init {
        // Monitor network connectivity and trigger sync when online
        scope.launch {
            networkMonitor.isConnected.collect { isConnected ->
                if (isConnected && pendingQueue.isNotEmpty()) {
                    Log.d(TAG, "Network restored, starting sync...")
                    processPendingOperations()
                }
            }
        }
    }

    /**
     * Queue a CREATE operation
     */
    fun queueCreateOperation(game: Game) {
        val operation = PendingOperation(
            id = System.currentTimeMillis(),
            gameLocalId = game.id,
            operationType = OperationType.CREATE,
            timestamp = System.currentTimeMillis(),
            gameData = gson.toJson(game)
        )

        pendingQueue.offer(operation)
        Log.d(TAG, "Queued CREATE operation for game ${game.id}")

        // Try to sync immediately if online
        if (networkMonitor.isCurrentlyConnected()) {
            scope.launch {
                processPendingOperations()
            }
        }
    }

    /**
     * Queue an UPDATE operation
     */
    fun queueUpdateOperation(game: Game, serverId: Int) {
        val operation = PendingOperation(
            id = System.currentTimeMillis(),
            gameLocalId = serverId,  // Use server ID for updates
            operationType = OperationType.UPDATE,
            timestamp = System.currentTimeMillis(),
            gameData = gson.toJson(game)
        )

        pendingQueue.offer(operation)
        Log.d(TAG, "Queued UPDATE operation for game server ID $serverId")

        // Try to sync immediately if online
        if (networkMonitor.isCurrentlyConnected()) {
            scope.launch {
                processPendingOperations()
            }
        }
    }

    /**
     * Queue a DELETE operation
     */
    fun queueDeleteOperation(serverId: Int) {
        val operation = PendingOperation(
            id = System.currentTimeMillis(),
            gameLocalId = serverId,  // Use server ID for deletes
            operationType = OperationType.DELETE,
            timestamp = System.currentTimeMillis(),
            gameData = null
        )

        pendingQueue.offer(operation)
        Log.d(TAG, "Queued DELETE operation for game server ID $serverId")

        // Try to sync immediately if online
        if (networkMonitor.isCurrentlyConnected()) {
            scope.launch {
                processPendingOperations()
            }
        }
    }

    /**
     * Process all pending operations in the queue
     * This will be called by ServerRepository when operations complete
     */
    suspend fun processPendingOperations() {
        if (_isSyncing.value) {
            Log.d(TAG, "Sync already in progress, skipping")
            return
        }

        if (pendingQueue.isEmpty()) {
            Log.d(TAG, "No pending operations to sync")
            return
        }

        if (!networkMonitor.isCurrentlyConnected()) {
            Log.d(TAG, "No network connection, cannot sync")
            return
        }

        _isSyncing.value = true
        _syncError.value = null

        Log.d(TAG, "Starting sync of ${pendingQueue.size} pending operations")

        try {
            // Note: Actual server calls will be made by ServerRepository
            // This manager just maintains the queue
            // ServerRepository will call removeOperation() after successful sync

            Log.d(TAG, "Sync process initiated")

        } catch (e: Exception) {
            Log.e(TAG, "Error during sync", e)
            _syncError.value = "Sync failed: ${e.message}"
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Remove an operation from the queue after successful sync
     */
    fun removeOperation(operationId: Long) {
        val removed = pendingQueue.removeIf { it.id == operationId }
        if (removed) {
            Log.d(TAG, "Removed operation $operationId from queue")
        }
    }

    /**
     * Get all pending operations (for ServerRepository to process)
     */
    fun getPendingOperations(): List<PendingOperation> {
        return pendingQueue.toList()
    }

    /**
     * Get count of pending operations
     */
    fun getPendingCount(): Int {
        return pendingQueue.size
    }

    /**
     * Clear all pending operations
     */
    fun clearQueue() {
        pendingQueue.clear()
        Log.d(TAG, "Cleared all pending operations")
    }

    /**
     * Clear sync error
     */
    fun clearError() {
        _syncError.value = null
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        pendingQueue.clear()
    }
}