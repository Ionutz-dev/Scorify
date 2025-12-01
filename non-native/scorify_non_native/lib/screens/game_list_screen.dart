import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../models/game.dart';
import '../viewmodels/game_list_viewmodel.dart';

class GameListScreen extends StatefulWidget {
  const GameListScreen({Key? key, required this.viewModel}) : super(key: key);

  final GameListViewModel viewModel;

  @override
  State<GameListScreen> createState() => _GameListScreenState();
}

class _GameListScreenState extends State<GameListScreen> {
  @override
  void initState() {
    super.initState();
    widget.viewModel.onLoadGames();
  }

  @override
  void dispose() {
    widget.viewModel.onDisposed();
    super.dispose();
  }

  void _deleteGame(BuildContext context, Game game) {
    showDialog(
      context: context,
      builder: (BuildContext dialogContext) {
        return AlertDialog(
          title: const Text('Delete Game'),
          content: Text(
              'Are you sure you want to delete ${game.homeTeam} vs ${game.awayTeam}?'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(),
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed: () {
                widget.viewModel.onDeleteGame(game.id);
                Navigator.of(dialogContext).pop();
              },
              child: const Text('Delete', style: TextStyle(color: Colors.red)),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F5),
      appBar: AppBar(
        backgroundColor: const Color(0xFF37474F),
        title: const Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'My Games',
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: Colors.white,
              ),
            ),
            Text(
              'Track your sports scores',
              style: TextStyle(
                fontSize: 14,
                color: Colors.grey,
              ),
            ),
          ],
        ),
      ),
      body: ListenableBuilder(
        listenable: widget.viewModel,
        builder: (context, child) {
          final state = widget.viewModel.gameListState;

          if (state == null || state.isLoading) {
            return const Center(
              child: CircularProgressIndicator(),
            );
          }

          if (state.error != null) {
            return Center(
              child: Text(
                'Error: ${state.error}',
                style: const TextStyle(color: Colors.red),
              ),
            );
          }

          final games = state.games;

          if (games.isEmpty) {
            return const Center(
              child: Text(
                'No games yet.\nTap + to add a game.',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 18, color: Colors.grey),
              ),
            );
          }

          return ListView.builder(
            padding: const EdgeInsets.all(8),
            itemCount: games.length,
            itemBuilder: (context, index) {
              final game = games[index];
              return Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: GameCard(
                  game: game,
                  onTap: () async {
                    await Navigator.of(context)
                        .pushNamed('/edit', arguments: game.id);
                    widget.viewModel.onRefresh();
                  },
                  onDelete: () => _deleteGame(context, game),
                ),
              );
            },
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          await Navigator.of(context).pushNamed('/add');
          widget.viewModel.onRefresh();
        },
        backgroundColor: const Color(0xFF26A69A),
        child: const Icon(Icons.add, color: Colors.white),
      ),
    );
  }
}

class GameCard extends StatelessWidget {
  final Game game;
  final VoidCallback onTap;
  final VoidCallback onDelete;

  const GameCard({
    Key? key,
    required this.game,
    required this.onTap,
    required this.onDelete,
  }) : super(key: key);

  Color _getStatusColor(String status) {
    switch (status) {
      case 'Completed':
        return const Color(0xFF26A69A);
      case 'In Progress':
        return const Color(0xFFEF5350);
      case 'Scheduled':
        return const Color(0xFFFFCA28);
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            children: [
              // Header row
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: const Color(0xFFE0F2F1),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          game.sportType,
                          style: const TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF546E7A),
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: _getStatusColor(game.status),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          game.status,
                          style: const TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                      ),
                    ],
                  ),
                  IconButton(
                    icon: const Icon(Icons.delete, color: Color(0xFFF44336)),
                    onPressed: onDelete,
                  ),
                ],
              ),
              const SizedBox(height: 12),
              // Score section
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  // Home team
                  Expanded(
                    child: Column(
                      children: [
                        Text(
                          game.homeTeam,
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                          textAlign: TextAlign.center,
                        ),
                        Text(
                          game.homeScore.toString(),
                          style: const TextStyle(
                            fontSize: 28,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF37474F),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const Padding(
                    padding: EdgeInsets.symmetric(horizontal: 16),
                    child: Text(
                      'VS',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF78909C),
                      ),
                    ),
                  ),
                  // Away team
                  Expanded(
                    child: Column(
                      children: [
                        Text(
                          game.awayTeam,
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                          textAlign: TextAlign.center,
                        ),
                        Text(
                          game.awayScore.toString(),
                          style: const TextStyle(
                            fontSize: 28,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF37474F),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              // Location and date
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    '📍 ${game.location}',
                    style: const TextStyle(fontSize: 12, color: Colors.grey),
                  ),
                  Text(
                    '📅 ${DateFormat('MMM dd, yyyy').format(game.date)}',
                    style: const TextStyle(fontSize: 12, color: Colors.grey),
                  ),
                ],
              ),
              // Add notes section
              if (game.notes.isNotEmpty) ...[
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: Colors.grey[100],
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Text(
                    game.notes,
                    style: const TextStyle(
                      fontSize: 12,
                      fontStyle: FontStyle.italic,
                      color: Colors.black87,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}