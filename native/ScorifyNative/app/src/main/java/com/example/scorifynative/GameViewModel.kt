package com.example.scorifynative

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository = GameRepository.getInstance(application)

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

    companion object {
        private const val TAG = "GameViewModel"
    }

    init {
        loadGames()
    }

    fun loadGames() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getAllGames()

            result.fold(
                onSuccess = { gamesList ->
                    _games.value = gamesList
                    Log.d(TAG, "Successfully loaded ${gamesList.size} games")
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

    fun addGame(game: Game) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            val result = repository.addGame(game)

            result.fold(
                onSuccess = { insertedGame ->
                    loadGames()
                    _successMessage.value = "Game added successfully!"
                    Log.d(TAG, "Successfully added game with id ${insertedGame.id}")
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

    fun updateGame(game: Game) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            val result = repository.updateGame(game)

            result.fold(
                onSuccess = {
                    loadGames()
                    _successMessage.value = "Game updated successfully!"
                    Log.d(TAG, "Successfully updated game with id ${game.id}")
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

    fun deleteGame(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            val result = repository.deleteGame(id)

            result.fold(
                onSuccess = {
                    loadGames()
                    _successMessage.value = "Game deleted successfully!"
                    Log.d(TAG, "Successfully deleted game with id $id")
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

    fun getGameById(id: Int): Game? {
        return _games.value.find { it.id == id }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
        Log.d(TAG, "ViewModel cleared, database closed")
    }
}