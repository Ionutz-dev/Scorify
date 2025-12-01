// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:scorify_non_native/main.dart';
import 'package:scorify_non_native/repositories/game_repository.dart';
import 'package:scorify_non_native/viewmodels/game_list_viewmodel.dart';
import 'package:scorify_non_native/viewmodels/add_edit_game_viewmodel.dart';

void main() {
  testWidgets('App smoke test', (WidgetTester tester) async {
    // Create dependencies
    final gameRepository = GameRepository();
    final gameListViewModel = GameListViewModel(gameRepository: gameRepository);
    final addEditGameViewModel = AddEditGameViewModel(gameRepository: gameRepository);

    // Build our app and trigger a frame
    await tester.pumpWidget(MyApp(
      gameListViewModel: gameListViewModel,
      addEditGameViewModel: addEditGameViewModel,
    ));

    // Verify that the app builds without crashing
    expect(find.text('My Games'), findsOneWidget);
  });
}
