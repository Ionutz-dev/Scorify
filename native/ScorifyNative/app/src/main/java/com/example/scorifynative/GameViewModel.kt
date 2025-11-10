package com.example.scorifynative

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel : ViewModel() {
    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games: StateFlow<List<Game>> = _games.asStateFlow()

    init {
        loadGames()
    }

    private fun loadGames() {
        _games.value = GameRepository.getAllGames()
    }

    fun addGame(game: Game) {
        GameRepository.addGame(game)
        loadGames()
    }

    fun updateGame(game: Game) {
        GameRepository.updateGame(game)
        loadGames()
    }

    fun deleteGame(id: Int) {
        GameRepository.deleteGame(id)
        loadGames()
    }

    fun getGameById(id: Int): Game? {
        return GameRepository.getGameById(id)
    }
}