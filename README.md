# Scorify

## Short Description

Scorify is a mobile application designed for sports enthusiasts who want to keep track of their favorite teams' games and scores in real-time. The app allows users to add games, record scores, track team statistics, and view historical match results even without an internet connection. Whether you're following local leagues or just want to maintain records of pickup games, this app provides an intuitive interface to manage all your sports data in one place.

## Domain Details

### Entity: Game

The main entity of the application is a **Game**, which represents a sports match between two teams.

**Fields:**

1. **id** (Integer) - Unique identifier for the game
2. **homeTeam** (String) - Name of the home team (required, max 50 characters)
3. **awayTeam** (String) - Name of the away team (required, max 50 characters)
4. **homeScore** (Integer) - Score of the home team (default: 0, min: 0)
5. **awayScore** (Integer) - Score of the away team (default: 0, min: 0)
6. **date** (DateTime) - Date and time when the game was played
7. **location** (String) - Venue where the game takes place (max 100 characters)
8. **sportType** (String) - Type of sport (e.g., "Basketball", "Football", "Tennis")
9. **status** (String) - Game status: "Scheduled", "In Progress", "Completed", "Cancelled"
10. **notes** (String) - Additional notes or comments about the game (optional, max 500 characters)

## CRUD Operations

### Create

**Description:** Users can add a new game by filling in all required information including team names, date, location, and sport type. The game is initially saved to the local database and then synchronized with the server when an internet connection is available.

**Offline Scenario:** When offline, the game is saved to the local SQLite database with a "pending sync" flag. Once the device reconnects to the internet, the app automatically uploads the new game to the server and updates the local record with the server-assigned ID.

### Read

**Description:** Users can view a list of all games, sorted by date (most recent first). Each game displays team names, scores, date, and status. Users can tap on any game to view detailed information including location, sport type, and notes.

**Offline Scenario:** When offline, the app loads all games from the local SQLite database. Users can browse through all previously synchronized games without any internet connection. A visual indicator shows which games have pending changes that haven't been synced yet.

### Update

**Description:** Users can edit existing games to update scores, change game status, modify date/time, or add notes. Common updates include recording final scores when a game ends or updating the status from "Scheduled" to "In Progress" or "Completed".

**Offline Scenario:** When offline, updates are saved to the local database and marked as "pending sync". The app maintains a sync queue that tracks all local modifications. When connectivity is restored, changes are pushed to the server in chronological order, with conflict resolution favoring the most recent timestamp.

### Delete

**Description:** Users can delete games from their tracking list. This might be used to remove duplicate entries, cancelled games, or games added by mistake. A confirmation dialog prevents accidental deletions.

**Offline Scenario:** When offline, the game is marked as "deleted" in the local database but not immediately removed. The deletion is queued for server synchronization. Once online, the app sends the delete request to the server, and only after successful confirmation is the record permanently removed from the local database.

## Persistence Details

### Local Database (SQLite)

-   All CRUD operations are first executed on the local SQLite database
-   Database schema includes a "games" table with all entity fields plus sync metadata
-   Additional fields for synchronization: `serverID`, `lastModified`, `syncStatus`
-   Provides instant access to data without network dependency

### Server Synchronization

The app implements a robust synchronization mechanism:

1. **Create Operations:** New games are posted to the server REST API endpoint `/api/games` with all game data. The server returns the assigned ID which updates the local record.

2. **Read Operations:** The app periodically fetches updates from the server endpoint `/api/games?since={lastSyncTime}` to retrieve games created or modified by other devices or users.

3. **Update Operations:** Modified games are sent via PUT request to `/api/games/{id}` with the complete updated game object. The server validates and stores the changes.

4. **Delete Operations:** Deletion requests are sent via DELETE to `/api/games/{id}`. The server removes the record and confirms successful deletion.

5. **Sync Queue:** A local queue manages all pending operations, ensuring data consistency even with intermittent connectivity. Operations are executed in order with automatic retry logic on failure.

## Offline Access Details

The app is designed to work seamlessly in offline mode:

-   **Complete Functionality:** All CRUD operations work without internet connection
-   **Automatic Sync:** When connectivity is restored, the app automatically synchronizes all pending changes in the background
-   **Conflict Resolution:** If the same game is modified on multiple devices, the app uses timestamp-based conflict resolution
-   **User Feedback:** Visual indicators (sync icons, status badges) inform users about sync status
-   **Data Integrity:** Local database transactions ensure data consistency even if the app crashes during offline operations

## Technology Stack

-   **Frontend:** React Native / Flutter for cross-platform mobile development
-   **Local Database:** SQLite for offline data persistence
-   **Backend:** RESTful API (Node.js/Express or Python/Flask)
-   **Server Database:** PostgreSQL or MongoDB
-   **State Management:** Redux or Provider for app state
-   **Network:** Axios/Fetch API for HTTP requests with retry logic

## Screenshots

<div align="center">

### Main Games List Screen

<img src="screenshot-list.png" width="400" alt="Scorify - Games List">

<br><br>

### Add/Edit Game Screen

<img src="screenshot-add.png" width="400" alt="Scorify - Add Game">

</div>
