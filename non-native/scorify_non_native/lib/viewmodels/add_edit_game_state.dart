import 'package:equatable/equatable.dart';
import '../models/game.dart';

class AddEditGameState extends Equatable {
  const AddEditGameState({
    this.game,
    this.isLoading = false,
    this.error,
  });

  final Game? game;
  final bool isLoading;
  final String? error;

  AddEditGameState copyWith({
    Game? game,
    bool? isLoading,
    String? error,
  }) {
    return AddEditGameState(
      game: game ?? this.game,
      isLoading: isLoading ?? this.isLoading,
      error: error ?? this.error,
    );
  }

  @override
  List<Object?> get props => [game, isLoading, error];
}