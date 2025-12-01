import '../models/game.dart';

class GameRepository {
  final List<Game> _games = [];
  int _nextId = 1;

  GameRepository() {
    // Initialize with fake data
    _games.addAll([
      Game(
        id: _nextId++,
        homeTeam: 'Lakers',
        awayTeam: 'Warriors',
        homeScore: 98,
        awayScore: 102,
        date: DateTime(2025, 10, 12, 19, 30),
        location: 'Staples Center',
        sportType: 'Basketball',
        status: 'Completed',
      ),
      Game(
        id: _nextId++,
        homeTeam: 'Real Madrid',
        awayTeam: 'Barcelona',
        homeScore: 1,
        awayScore: 3,
        date: DateTime(2025, 10, 14, 20, 0),
        location: 'Santiago Bernabéu',
        sportType: 'Football',
        status: 'In Progress',
      ),
      Game(
        id: _nextId++,
        homeTeam: 'Djokovic',
        awayTeam: 'Alcaraz',
        homeScore: 0,
        awayScore: 0,
        date: DateTime(2025, 10, 20, 15, 0),
        location: 'Wimbledon',
        sportType: 'Tennis',
        status: 'Scheduled',
      ),
    ]);
  }

  List<Game> getAllGames() {
    return List.unmodifiable(_games);
  }

  Game? getGameById(int id) {
    try {
      return _games.firstWhere((game) => game.id == id);
    } catch (e) {
      return null;
    }
  }

  Game addGame(Game game) {
    final newGame = game.copyWith(id: _nextId++);
    _games.add(newGame);
    return newGame;
  }

  bool updateGame(Game game) {
    final index = _games.indexWhere((g) => g.id == game.id);
    if (index != -1) {
      _games[index] = game;
      return true;
    }
    return false;
  }

  bool deleteGame(int id) {
    final initialLength = _games.length;
    _games.removeWhere((game) => game.id == id);
    return _games.length < initialLength;
  }
}