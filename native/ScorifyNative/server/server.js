const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const cors = require('cors');
const bodyParser = require('body-parser');
const fs = require('fs');
const path = require('path');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

const PORT = 3000;
const DB_FILE = path.join(__dirname, 'games.json');

// Middleware
app.use(cors());
app.use(bodyParser.json());

// Logging middleware
app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
    next();
});

// Initialize database file if it doesn't exist
if (!fs.existsSync(DB_FILE)) {
    fs.writeFileSync(DB_FILE, JSON.stringify({ games: [], nextId: 1 }, null, 2));
    console.log('[SERVER] Created games.json database file');
}

// Helper function to read database
function readDatabase() {
    try {
        const data = fs.readFileSync(DB_FILE, 'utf8');
        return JSON.parse(data);
    } catch (error) {
        console.error('[SERVER ERROR] Failed to read database:', error);
        return { games: [], nextId: 1 };
    }
}

// Helper function to write database
function writeDatabase(data) {
    try {
        fs.writeFileSync(DB_FILE, JSON.stringify(data, null, 2));
        console.log('[SERVER] Database updated successfully');
    } catch (error) {
        console.error('[SERVER ERROR] Failed to write database:', error);
        throw error;
    }
}

// Broadcast to all WebSocket clients
function broadcast(message) {
    const messageStr = JSON.stringify(message);
    console.log('[WEBSOCKET] Broadcasting:', messageStr);

    wss.clients.forEach(client => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(messageStr);
        }
    });
}

// WebSocket connection handler
wss.on('connection', (ws) => {
    console.log('[WEBSOCKET] New client connected');

    ws.on('message', (message) => {
        console.log('[WEBSOCKET] Received:', message.toString());
    });

    ws.on('close', () => {
        console.log('[WEBSOCKET] Client disconnected');
    });

    ws.on('error', (error) => {
        console.error('[WEBSOCKET ERROR]', error);
    });
});

// REST API Endpoints

// GET /api/games - Get all games
app.get('/api/games', (req, res) => {
    try {
        const db = readDatabase();
        console.log(`[SERVER] Retrieved ${db.games.length} games`);
        res.json({
            success: true,
            data: db.games
        });
    } catch (error) {
        console.error('[SERVER ERROR] GET /api/games failed:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to retrieve games'
        });
    }
});

// GET /api/games/:id - Get single game
app.get('/api/games/:id', (req, res) => {
    try {
        const db = readDatabase();
        const gameId = parseInt(req.params.id);
        const game = db.games.find(g => g.id === gameId);

        if (!game) {
            console.log(`[SERVER] Game with id ${gameId} not found`);
            return res.status(404).json({
                success: false,
                message: 'Game not found'
            });
        }

        console.log(`[SERVER] Retrieved game with id ${gameId}`);
        res.json({
            success: true,
            data: game
        });
    } catch (error) {
        console.error('[SERVER ERROR] GET /api/games/:id failed:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to retrieve game'
        });
    }
});

// POST /api/games - Create new game
app.post('/api/games', (req, res) => {
    try {
        const db = readDatabase();
        const gameData = req.body;

        // Validation
        if (!gameData.homeTeam || !gameData.awayTeam || !gameData.location) {
            console.log('[SERVER] Validation failed: missing required fields');
            return res.status(400).json({
                success: false,
                message: 'Missing required fields: homeTeam, awayTeam, location'
            });
        }

        // Create new game with server-assigned ID
        const newGame = {
            id: db.nextId,
            homeTeam: gameData.homeTeam,
            awayTeam: gameData.awayTeam,
            homeScore: gameData.homeScore || 0,
            awayScore: gameData.awayScore || 0,
            date: gameData.date || Date.now(),
            location: gameData.location,
            sportType: gameData.sportType || 'Football',
            status: gameData.status || 'Scheduled',
            notes: gameData.notes || ''
        };

        db.games.push(newGame);
        db.nextId++;
        writeDatabase(db);

        console.log(`[SERVER] Created game with id ${newGame.id}`);

        // Broadcast to all connected clients
        broadcast({
            type: 'CREATE',
            data: newGame
        });

        res.status(201).json({
            success: true,
            data: newGame
        });
    } catch (error) {
        console.error('[SERVER ERROR] POST /api/games failed:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to create game'
        });
    }
});

// PUT /api/games/:id - Update game
app.put('/api/games/:id', (req, res) => {
    try {
        const db = readDatabase();
        const gameId = parseInt(req.params.id);
        const gameIndex = db.games.findIndex(g => g.id === gameId);

        if (gameIndex === -1) {
            console.log(`[SERVER] Game with id ${gameId} not found for update`);
            return res.status(404).json({
                success: false,
                message: 'Game not found'
            });
        }

        const gameData = req.body;

        // Validation
        if (!gameData.homeTeam || !gameData.awayTeam || !gameData.location) {
            console.log('[SERVER] Validation failed: missing required fields');
            return res.status(400).json({
                success: false,
                message: 'Missing required fields: homeTeam, awayTeam, location'
            });
        }

        // Update game (preserve the ID)
        const updatedGame = {
            id: gameId,
            homeTeam: gameData.homeTeam,
            awayTeam: gameData.awayTeam,
            homeScore: gameData.homeScore || 0,
            awayScore: gameData.awayScore || 0,
            date: gameData.date || db.games[gameIndex].date,
            location: gameData.location,
            sportType: gameData.sportType || 'Football',
            status: gameData.status || 'Scheduled',
            notes: gameData.notes || ''
        };

        db.games[gameIndex] = updatedGame;
        writeDatabase(db);

        console.log(`[SERVER] Updated game with id ${gameId}`);

        // Broadcast to all connected clients
        broadcast({
            type: 'UPDATE',
            data: updatedGame
        });

        res.json({
            success: true,
            data: updatedGame
        });
    } catch (error) {
        console.error('[SERVER ERROR] PUT /api/games/:id failed:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to update game'
        });
    }
});

// DELETE /api/games/:id - Delete game
app.delete('/api/games/:id', (req, res) => {
    try {
        const db = readDatabase();
        const gameId = parseInt(req.params.id);
        const gameIndex = db.games.findIndex(g => g.id === gameId);

        if (gameIndex === -1) {
            console.log(`[SERVER] Game with id ${gameId} not found for deletion`);
            return res.status(404).json({
                success: false,
                message: 'Game not found'
            });
        }

        const deletedGame = db.games[gameIndex];
        db.games.splice(gameIndex, 1);
        writeDatabase(db);

        console.log(`[SERVER] Deleted game with id ${gameId}`);

        // Broadcast to all connected clients
        broadcast({
            type: 'DELETE',
            data: { id: gameId }
        });

        res.json({
            success: true,
            message: 'Game deleted successfully',
            data: { id: gameId }
        });
    } catch (error) {
        console.error('[SERVER ERROR] DELETE /api/games/:id failed:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to delete game'
        });
    }
});

// Health check endpoint
app.get('/api/health', (req, res) => {
    res.json({
        success: true,
        message: 'Server is running',
        timestamp: new Date().toISOString()
    });
});

// Start server
server.listen(PORT, () => {
    console.log('='.repeat(50));
    console.log(`[SERVER] Scorify Server started successfully`);
    console.log(`[SERVER] HTTP API running on http://localhost:${PORT}`);
    console.log(`[SERVER] WebSocket running on ws://localhost:${PORT}`);
    console.log(`[SERVER] Database file: ${DB_FILE}`);
    console.log('='.repeat(50));
});

// Graceful shutdown
process.on('SIGINT', () => {
    console.log('\n[SERVER] Shutting down gracefully...');
    server.close(() => {
        console.log('[SERVER] Server closed');
        process.exit(0);
    });
});