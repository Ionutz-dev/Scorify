package com.example.scorifynative.sync

/**
 * Represents a pending operation to be synced with the server
 * Used for offline support - operations are queued and executed when online
 */
data class PendingOperation(
    val id: Long,                    // Local database ID for the pending operation
    val gameLocalId: Int,            // Local ID of the game
    val operationType: OperationType, // CREATE, UPDATE, DELETE
    val timestamp: Long,             // When the operation was created
    val gameData: String? = null     // JSON string of game data (null for DELETE)
)

/**
 * Types of operations that can be pending
 */
enum class OperationType {
    CREATE,
    UPDATE,
    DELETE
}