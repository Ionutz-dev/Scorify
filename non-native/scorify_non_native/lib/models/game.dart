class Game {
  final int id;
  final String homeTeam;
  final String awayTeam;
  final int homeScore;
  final int awayScore;
  final DateTime date;
  final String location;
  final String sportType;
  final String status;
  final String notes;

  Game({
    required this.id,
    required this.homeTeam,
    required this.awayTeam,
    required this.homeScore,
    required this.awayScore,
    required this.date,
    required this.location,
    required this.sportType,
    required this.status,
    this.notes = '',
  });

  Game copyWith({
    int? id,
    String? homeTeam,
    String? awayTeam,
    int? homeScore,
    int? awayScore,
    DateTime? date,
    String? location,
    String? sportType,
    String? status,
    String? notes,
  }) {
    return Game(
      id: id ?? this.id,
      homeTeam: homeTeam ?? this.homeTeam,
      awayTeam: awayTeam ?? this.awayTeam,
      homeScore: homeScore ?? this.homeScore,
      awayScore: awayScore ?? this.awayScore,
      date: date ?? this.date,
      location: location ?? this.location,
      sportType: sportType ?? this.sportType,
      status: status ?? this.status,
      notes: notes ?? this.notes,
    );
  }
}