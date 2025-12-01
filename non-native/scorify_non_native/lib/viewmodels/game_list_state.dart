import 'package:equatable/equatable.dart';
import '../models/game.dart';

class GameListState extends Equatable {
  const GameListState({
    required this.games,
    this.isLoading = false,
    this.error,
  });

  final List<Game> games;
  final bool isLoading;
  final String? error;

  GameListState copyWith({
    List<Game>? games,
    bool? isLoading,
    String? error,
  }) {
    return GameListState(
      games: games ?? this.games,
      isLoading: isLoading ?? this.isLoading,
      error: error ?? this.error,
    );
  }

  @override
  List<Object?> get props => [games, isLoading, error];
}