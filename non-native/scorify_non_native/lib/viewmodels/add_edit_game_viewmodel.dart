import 'package:flutter/material.dart';
import '../models/game.dart';
import '../repositories/game_repository.dart';
import 'add_edit_game_state.dart';

class AddEditGameViewModel extends ChangeNotifier {
  AddEditGameViewModel({required this.gameRepository});

  final GameRepository gameRepository;

  // State of the Add/Edit Game Screen
  AddEditGameState? addEditGameState;

  // Load game for editing
  void onLoadGame(int gameId) async {
    addEditGameState = const AddEditGameState(isLoading: true);
    notifyListeners();

    // Simulate network delay
    await Future.delayed(const Duration(milliseconds: 300));

    final game = gameRepository.getGameById(gameId);
    addEditGameState = AddEditGameState(game: game, isLoading: false);
    notifyListeners();
  }

  // Initialize for adding new game
  void onInitializeNewGame() {
    addEditGameState = const AddEditGameState(isLoading: false);
    notifyListeners();
  }

  // Save game (add or update)
  void onSaveGame(Game game, bool isEditing) {
    if (isEditing) {
      gameRepository.updateGame(game);
    } else {
      gameRepository.addGame(game);
    }
  }

  void onDisposed() {
    addEditGameState = null;
    notifyListeners();
  }
}