import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'viewmodels/game_viewmodel.dart';
import 'screens/game_list_screen.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (context) => GameViewModel(),
      child: MaterialApp(
        title: 'Scorify',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          primarySwatch: Colors.blueGrey,
          useMaterial3: true,
        ),
        home: const GameListScreen(),
      ),
    );
  }
}