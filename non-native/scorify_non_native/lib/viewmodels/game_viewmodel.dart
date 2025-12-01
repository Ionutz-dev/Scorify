import 'package:flutter/foundation.dart';
import '../models/game.dart';
import '../repositories/game_repository.dart';

class GameViewModel extends ChangeNotifier {
  final GameRepository _repository = GameRepository();

  List<Game> get games => _repository.getAllGames();

  void addGame(Game game) {
    _repository.addGame(game);
    notifyListeners();
  }

  void updateGame(Game game) {
    _repository.updateGame(game);
    notifyListeners();
  }

  void deleteGame(int id) {
    _repository.deleteGame(id);
    notifyListeners();
  }

  Game? getGameById(int id) {
    return _repository.getGameById(id);
  }
}