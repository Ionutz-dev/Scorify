import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import '../models/game.dart';
import '../viewmodels/add_edit_game_viewmodel.dart';

class AddEditGameScreen extends StatefulWidget {
  const AddEditGameScreen({Key? key, required this.viewModel})
      : super(key: key);

  final AddEditGameViewModel viewModel;

  @override
  State<AddEditGameScreen> createState() => _AddEditGameScreenState();
}

class _AddEditGameScreenState extends State<AddEditGameScreen> {
  final _formKey = GlobalKey<FormState>();
  final _homeTeamController = TextEditingController();
  final _awayTeamController = TextEditingController();
  final _homeScoreController = TextEditingController();
  final _awayScoreController = TextEditingController();
  final _locationController = TextEditingController();
  final _notesController = TextEditingController();

  String _sportType = 'Basketball';
  String _status = 'Scheduled';
  DateTime _selectedDateTime = DateTime.now();
  int? _gameId;

  final List<String> _sportTypes = [
    'Basketball',
    'Football',
    'Tennis',
    'Baseball',
    'Hockey'
  ];
  final List<String> _statuses = ['Scheduled', 'In Progress', 'Completed'];

  bool get isEditing => _gameId != null;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    // Get game ID from route arguments
    final gameId = ModalRoute.of(context)!.settings.arguments as int?;

    if (gameId != null) {
      _gameId = gameId;
      widget.viewModel.onLoadGame(gameId);
    } else {
      widget.viewModel.onInitializeNewGame();
    }
  }

  @override
  void dispose() {
    _homeTeamController.dispose();
    _awayTeamController.dispose();
    _homeScoreController.dispose();
    _awayScoreController.dispose();
    _locationController.dispose();
    _notesController.dispose();
    widget.viewModel.onDisposed();
    super.dispose();
  }

  void _populateFields(Game game) {
    _homeTeamController.text = game.homeTeam;
    _awayTeamController.text = game.awayTeam;
    _homeScoreController.text = game.homeScore.toString();
    _awayScoreController.text = game.awayScore.toString();
    _locationController.text = game.location;
    _notesController.text = game.notes;
    _sportType = game.sportType;
    _status = game.status;
    _selectedDateTime = game.date;
  }

  Future<void> _selectDateTime(BuildContext context) async {
    final DateTime? pickedDate = await showDatePicker(
      context: context,
      initialDate: _selectedDateTime,
      firstDate: DateTime(2000),
      lastDate: DateTime(2100),
    );

    if (pickedDate != null) {
      final TimeOfDay? pickedTime = await showTimePicker(
        context: context,
        initialTime: TimeOfDay.fromDateTime(_selectedDateTime),
      );

      if (pickedTime != null) {
        setState(() {
          _selectedDateTime = DateTime(
            pickedDate.year,
            pickedDate.month,
            pickedDate.day,
            pickedTime.hour,
            pickedTime.minute,
          );
        });
      }
    }
  }

  void _saveGame() {
    if (_formKey.currentState!.validate()) {
      final game = Game(
        id: _gameId ?? 0,
        homeTeam: _homeTeamController.text.trim(),
        awayTeam: _awayTeamController.text.trim(),
        homeScore: int.parse(_homeScoreController.text),
        awayScore: int.parse(_awayScoreController.text),
        date: _selectedDateTime,
        location: _locationController.text.trim(),
        sportType: _sportType,
        status: _status,
        notes: _notesController.text.trim(),
      );

      widget.viewModel.onSaveGame(game, isEditing);
      Navigator.pop(context);
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: widget.viewModel,
      builder: (context, child) {
        final state = widget.viewModel.addEditGameState;

        if (state == null || state.isLoading) {
          return Scaffold(
            appBar: AppBar(
              backgroundColor: const Color(0xFF37474F),
              title: const Text('Loading...', style: TextStyle(color: Colors.white)),
              leading: IconButton(
                icon: const Icon(Icons.arrow_back, color: Colors.white),
                onPressed: () => Navigator.pop(context),
              ),
            ),
            body: const Center(child: CircularProgressIndicator()),
          );
        }

        // Populate fields if editing and not yet populated
        if (state.game != null && _homeTeamController.text.isEmpty) {
          _populateFields(state.game!);
        }

        return Scaffold(
          backgroundColor: Colors.white,
          appBar: AppBar(
            backgroundColor: const Color(0xFF37474F),
            title: Text(
              isEditing ? 'Edit Game' : 'Add Game',
              style: const TextStyle(color: Colors.white),
            ),
            leading: IconButton(
              icon: const Icon(Icons.arrow_back, color: Colors.white),
              onPressed: () => Navigator.pop(context),
            ),
          ),
          body: SingleChildScrollView(
            padding: const EdgeInsets.all(16.0),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  DropdownButtonFormField<String>(
                    value: _sportType,
                    decoration: const InputDecoration(
                      labelText: 'Sport Type',
                      border: OutlineInputBorder(),
                    ),
                    items: _sportTypes.map((sport) {
                      return DropdownMenuItem(
                          value: sport, child: Text(sport));
                    }).toList(),
                    onChanged: (value) {
                      setState(() {
                        _sportType = value!;
                      });
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _homeTeamController,
                    decoration: const InputDecoration(
                      labelText: 'Home Team',
                      hintText: 'Enter home team name',
                      border: OutlineInputBorder(),
                    ),
                    validator: (value) {
                      if (value == null || value.trim().isEmpty) {
                        return 'Please enter home team name';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _awayTeamController,
                    decoration: const InputDecoration(
                      labelText: 'Away Team',
                      hintText: 'Enter away team name',
                      border: OutlineInputBorder(),
                    ),
                    validator: (value) {
                      if (value == null || value.trim().isEmpty) {
                        return 'Please enter away team name';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      Expanded(
                        child: TextFormField(
                          controller: _homeScoreController,
                          decoration: const InputDecoration(
                            labelText: 'Home Score',
                            border: OutlineInputBorder(),
                          ),
                          keyboardType: TextInputType.number,
                          inputFormatters: [
                            FilteringTextInputFormatter.digitsOnly
                          ],
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'Required';
                            }
                            return null;
                          },
                        ),
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: TextFormField(
                          controller: _awayScoreController,
                          decoration: const InputDecoration(
                            labelText: 'Away Score',
                            border: OutlineInputBorder(),
                          ),
                          keyboardType: TextInputType.number,
                          inputFormatters: [
                            FilteringTextInputFormatter.digitsOnly
                          ],
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'Required';
                            }
                            return null;
                          },
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  InkWell(
                    onTap: () => _selectDateTime(context),
                    child: InputDecorator(
                      decoration: const InputDecoration(
                        labelText: 'Date & Time',
                        border: OutlineInputBorder(),
                      ),
                      child: Text(
                        DateFormat('MM/dd/yyyy, HH:mm')
                            .format(_selectedDateTime),
                        style: const TextStyle(fontSize: 16),
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _locationController,
                    decoration: const InputDecoration(
                      labelText: 'Location',
                      hintText: 'Enter location',
                      border: OutlineInputBorder(),
                    ),
                    validator: (value) {
                      if (value == null || value.trim().isEmpty) {
                        return 'Please enter location';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<String>(
                    value: _status,
                    decoration: const InputDecoration(
                      labelText: 'Status',
                      border: OutlineInputBorder(),
                    ),
                    items: _statuses.map((status) {
                      return DropdownMenuItem(
                          value: status, child: Text(status));
                    }).toList(),
                    onChanged: (value) {
                      setState(() {
                        _status = value!;
                      });
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _notesController,
                    decoration: const InputDecoration(
                      labelText: 'Notes',
                      hintText: 'Additional notes (optional)',
                      border: OutlineInputBorder(),
                    ),
                    maxLines: 3,
                  ),
                  const SizedBox(height: 24),
                  ElevatedButton(
                    onPressed: _saveGame,
                    style: ElevatedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      backgroundColor: const Color(0xFF26A69A),
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                    child: Text(
                      isEditing ? 'Update' : 'Add Game',
                      style: const TextStyle(fontSize: 16),
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}