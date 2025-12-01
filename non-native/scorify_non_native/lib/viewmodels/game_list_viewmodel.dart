import 'package:flutter/material.dart';
import '../repositories/game_repository.dart';
import 'game_list_state.dart';


class GameListViewModel extends ChangeNotifier {
  GameListViewModel({required this.gameRepository});

  final GameRepository gameRepository;

  // State of the Game List Screen
  GameListState? gameListState;

  // Load all games
  void onLoadGames() async {
    gameListState = GameListState(games: [], isLoading: true);
    notifyListeners();

    // Simulate network delay
    await Future.delayed(const Duration(milliseconds: 500));

    final games = gameRepository.getAllGames();
    gameListState = GameListState(games: games, isLoading: false);
    notifyListeners();
  }

  // Delete a game
  void onDeleteGame(int id) {
    final success = gameRepository.deleteGame(id);
    if (success) {
      onLoadGames();
    }
  }

  // Refresh games list
  void onRefresh() {
    onLoadGames();
  }

  void onDisposed() {
    gameListState = null;
    notifyListeners();
  }
}