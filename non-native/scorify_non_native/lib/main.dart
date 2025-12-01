import 'package:flutter/material.dart';
import 'repositories/game_repository.dart';
import 'viewmodels/game_list_viewmodel.dart';
import 'viewmodels/add_edit_game_viewmodel.dart';
import 'screens/game_list_screen.dart';
import 'screens/add_edit_game_screen.dart';

/// Main entry point of the application
void main() {
  // Initialize dependencies
  final gameRepository = GameRepository();

  // Create ViewModels
  final gameListViewModel = GameListViewModel(gameRepository: gameRepository);
  final addEditGameViewModel =
  AddEditGameViewModel(gameRepository: gameRepository);

  runApp(
    MyApp(
      gameListViewModel: gameListViewModel,
      addEditGameViewModel: addEditGameViewModel,
    ),
  );
}

class MyApp extends StatelessWidget {
  const MyApp({
    Key? key,
    required this.gameListViewModel,
    required this.addEditGameViewModel,
  }) : super(key: key);

  final GameListViewModel gameListViewModel;
  final AddEditGameViewModel addEditGameViewModel;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Scorify',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        primarySwatch: Colors.blueGrey,
        useMaterial3: true,
      ),
      initialRoute: '/home',
      routes: {
        '/home': (context) =>
            GameListScreen(viewModel: gameListViewModel),
        '/add': (context) =>
            AddEditGameScreen(viewModel: addEditGameViewModel),
        '/edit': (context) =>
            AddEditGameScreen(viewModel: addEditGameViewModel),
      },
    );
  }
}