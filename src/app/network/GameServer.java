package app.network;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import app.network.NetworkMessage.GameResult;
import app.network.NetworkMessage.LobbyPlayer;
import app.network.NetworkMessage.PlayerState;
public class GameServer {

    // ------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------

    public static final int DEFAULT_PORT    = 5555;
    public static final int MIN_PLAYERS     = 2;
    public static final int MAX_PLAYERS     = 4;

    /** Game duration in seconds. Must match GamePlayScreen timer. */
    private static final int GAME_DURATION_SECONDS = 40;

    /** How often the server broadcasts a GAME_STATE snapshot (milliseconds). */
    private static final long BROADCAST_INTERVAL_MS = 50; // ~20 Hz

    /**
     * Dough colors assigned in join order.
     * Matches the color map in GamePlayScreen so sprites stay consistent.
     */
    private static final String[] PLAYER_COLORS = {
        "#FF7043", // orange
        "#1E88E5", // blue
        "#43A047", // green
        "#E53935", // red
        "#FDD835", // yellow
        "#EC407A", // pink
        "#8E24AA", // purple
        "#3949AB"  // indigo
    };

    /**
     * Spawn angles (degrees) for up to 8 players, evenly distributed around
     * the arena so no two players start adjacent.
     */
    private static final double[] SPAWN_ANGLES_DEG = {
        0, 180, 90, 270,   // 4 players: E, W, N, S
        45, 225, 135, 315  // 5-8 players: NE, SW, NW, SE
    };

    /** Spawn radius — 60 % of the arena radius. */
    private static final double SPAWN_RADIUS = 1500 * 0.60; // = 900

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    private final int port;

    /** All currently connected handlers. Thread-safe list for iteration. */
    private final CopyOnWriteArrayList<ClientHandler> clients =
            new CopyOnWriteArrayList<>();

    /**
     * Latest position snapshot per playerId.
     * Written by ClientHandler threads; read by the broadcast scheduler.
     */
    private final Map<Integer, PlayerState> latestStates =
            Collections.synchronizedMap(new LinkedHashMap<>());

    /** Set of playerIds that have signalled "ready" in the lobby. */
    private final List<Integer> readyPlayers = new CopyOnWriteArrayList<>();

    private final AtomicInteger nextPlayerId = new AtomicInteger(1);

    private ServerSocket serverSocket;
    private volatile boolean accepting = true;  // accept-loop flag
    private volatile boolean gameStarted = false;
    private volatile boolean gameEnded  = false;

    /** True while a server is bound to the port. Checked by MultiplayerScreen to prevent double-bind. */
    private static volatile boolean serverRunning = false;

    public static boolean isServerRunning() { return serverRunning; }

    private ScheduledExecutorService broadcastScheduler;

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    public GameServer(int port) {
        this.port = port;
    }

    public GameServer() {
        this(DEFAULT_PORT);
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            serverRunning = true;
            System.out.println("[Server] Listening on port " + port);

            while (accepting) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    if (gameStarted) {
                        sendRejection(clientSocket, "Game has already started.");
                        clientSocket.close();
                        continue;
                    }

                    if (clients.size() >= MAX_PLAYERS) {
                        System.out.println("[Server] Lobby full — rejected a connection.");
                        sendRejection(clientSocket, "Game is full. Maximum " + MAX_PLAYERS + " players allowed.");
                        clientSocket.close();
                        continue;
                    }

                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    clients.add(handler);
                    new Thread(handler, "ClientHandler-" + clients.size()).start();

                } catch (IOException e) {
                    if (accepting) {
                        System.out.println("[Server] Accept error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[Server] Could not open port " + port + ": " + e.getMessage());
        }
    }

    //Stops the server
    public void stop() {
        serverRunning = false;
        accepting = false;
        if (broadcastScheduler != null) broadcastScheduler.shutdownNow();
        for (ClientHandler c : clients) c.disconnect();
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        System.out.println("[Server] Stopped.");
    }

    // ------------------------------------------------------------------
    // Event handlers — called by ClientHandler threads
    // ------------------------------------------------------------------

    public synchronized void onPlayerJoin(ClientHandler handler) {
        int id = nextPlayerId.getAndIncrement();
        handler.setPlayerId(id);

        String color = PLAYER_COLORS[(id - 1) % PLAYER_COLORS.length];

        // Send the assigned id/color back to this client as a LOBBY_UPDATE
        // (the client reads its own id from the first slot matching its name)
        System.out.println("[Server] Player joined: " + handler.getPlayerName() + " → id=" + id + " color=" + color);

        // Seed an empty state so the player appears in broadcasts immediately
        latestStates.put(id, new PlayerState(
            id, handler.getPlayerName(), color, 0, 0, 1, 0, new double[0], false, 0.0
        ));

        broadcastLobbyUpdate();
    }

    //Ready toggle
    public synchronized void onPlayerReady(ClientHandler handler) {
        int id = handler.getPlayerId();
        if (!readyPlayers.contains(id)) {
            readyPlayers.add(id);
        } else {
            readyPlayers.remove((Integer) id); // toggle off
        }
        System.out.println("[Server] Player " + id + " ready=" + readyPlayers.contains(id));
        broadcastLobbyUpdate();
        checkStartCondition();
    }

    // Chat Message
    public void onChat(ClientHandler handler, NetworkMessage msg) {
        int id = handler.getPlayerId();
        PlayerState state = latestStates.get(id);
        if (state != null) {
            msg.colorHex = state.colorHex;
        } else {
            msg.colorHex = "#FFFFFF";
        }
        System.out.println("[Chat] " + msg.playerName + ": " + msg.message);
        broadcast(msg);
    }

    //Position Update
    public void onPositionUpdate(ClientHandler handler, NetworkMessage msg) {
        if (!gameStarted) return;

        int id = handler.getPlayerId();
        PlayerState existing = latestStates.get(id);
        String color  = existing != null ? existing.colorHex  : "#FFFFFF";
        String name   = existing != null ? existing.playerName : handler.getPlayerName();

        latestStates.put(id, new PlayerState(
            id, name, color,
            msg.x, msg.y, msg.dirX, msg.dirY,
            msg.trailPoints != null ? msg.trailPoints : new double[0],
            false,
            msg.territoryPercent
        ));
    }

    // A client's socket closed (crash, disconnect, etc.)
    public synchronized void onClientDisconnected(ClientHandler handler) {
        clients.remove(handler);
        int id = handler.getPlayerId();
        if (id != -1) {
            latestStates.remove(id);
            readyPlayers.remove((Integer) id);
            System.out.println("[Server] Player " + id + " left the game.");
            if (!gameStarted) {
                broadcastLobbyUpdate();
            } else {
                broadcast(NetworkMessage.playerDied(id));
                // Last-player-standing: if only one client remains, end the game now.
                checkLastPlayerStanding();
            }
        }
    }

    /**
     * Called after a player dies/disconnects during a game.
     * If only one live player remains, trigger endGame() immediately so the
     * winner is determined right away rather than waiting for the timer.
     */
    private void checkLastPlayerStanding() {
        if (!gameStarted) return;
        long alive = clients.stream().filter(c -> c.getPlayerId() != -1).count();
        if (alive <= 1) {
            System.out.println("[Server] Last player standing — ending game early.");
            // Run on a new thread so we don't deadlock inside the synchronized block.
            new Thread(this::endGame, "EndGameThread").start();
        }
    }

    // Lobby helpers
    // Broadcasts the current lobby roster to every connected client.
    private void broadcastLobbyUpdate() {
        List<LobbyPlayer> roster = buildRoster();
        NetworkMessage msg = NetworkMessage.lobbyUpdate(roster);
        // Also embed each client's own playerId so they can self-identify.
        // We send a per-client copy with playerId set.
        for (ClientHandler c : clients) {
            NetworkMessage personal = NetworkMessage.lobbyUpdate(roster);
            personal.playerId = c.getPlayerId();
            c.send(personal);
        }
    }

    private List<LobbyPlayer> buildRoster() {
        List<LobbyPlayer> list = new ArrayList<>();
        for (ClientHandler c : clients) {
            int id = c.getPlayerId();
            if (id == -1) continue; // not yet joined
            PlayerState state = latestStates.get(id);
            String color = state != null ? state.colorHex : "#FFFFFF";
            list.add(new LobbyPlayer(id, c.getPlayerName(), color, readyPlayers.contains(id)));
        }
        return list;
    }

    
    //Checks whether all connected players (≥ MIN_PLAYERS) are ready.
    //If so, kicks off the game.
    private void checkStartCondition() {
        if (gameStarted) return;
        int connected = (int) clients.stream().filter(c -> c.getPlayerId() != -1).count();
        if (connected >= MIN_PLAYERS && readyPlayers.size() >= connected) {
            startGame();
        }
    }

    // Game start
    private synchronized void startGame() {
        if (gameStarted) return;
        gameStarted = true;
        System.out.println("[Server] Starting game with " + clients.size() + " players!");

        List<LobbyPlayer> roster = buildRoster();

        int slot = 0;
        for (ClientHandler c : clients) {
            int id = c.getPlayerId();
            if (id == -1) continue;

            // Assign spawn position based on join order
            double angleDeg = SPAWN_ANGLES_DEG[slot % SPAWN_ANGLES_DEG.length];
            double angleRad = Math.toRadians(angleDeg);
            double spawnX   = Math.cos(angleRad) * SPAWN_RADIUS;
            double spawnY   = Math.sin(angleRad) * SPAWN_RADIUS;

            PlayerState state = latestStates.get(id);
            String color = state != null ? state.colorHex : "#FFFFFF";

            // Update stored state with proper spawn coords
            latestStates.put(id, new PlayerState(
                id, c.getPlayerName(), color,
                spawnX, spawnY, 0, -1, new double[0], false, 0.0
            ));

            c.send(NetworkMessage.startGame(id, spawnX, spawnY, color, roster));
            slot++;
        }

        // Start the state-broadcast loop (~20 Hz)
        broadcastScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BroadcastLoop");
            t.setDaemon(true);
            return t;
        });
        broadcastScheduler.scheduleAtFixedRate(
            this::broadcastGameState,
            0, BROADCAST_INTERVAL_MS, TimeUnit.MILLISECONDS
        );

        // Schedule game-over after the configured duration
        broadcastScheduler.schedule(
            this::endGame,
            GAME_DURATION_SECONDS, TimeUnit.SECONDS
        );
    }

    // In-game broadcast
    private void broadcastGameState() {
        List<PlayerState> snapshot;
        synchronized (latestStates) {
            snapshot = new ArrayList<>(latestStates.values());
        }
        broadcast(NetworkMessage.gameState(snapshot));
    }

    // Game over
    private synchronized void endGame() {
        if (gameEnded) return; // guard against timer + last-standing double-fire
        gameEnded = true;
        System.out.println("[Server] Game over — computing results.");
        if (broadcastScheduler != null) broadcastScheduler.shutdown();

        List<GameResult> results = new ArrayList<>();
        synchronized (latestStates) {
            for (PlayerState s : latestStates.values()) {
                results.add(new GameResult(s.playerId, s.playerName, s.colorHex, s.territoryPercent, 0));
            }
        }

        // Sort descending by territory, assign ranks
        results.sort((a, b) -> Double.compare(b.territoryPercent, a.territoryPercent));
        for (int i = 0; i < results.size(); i++) {
            results.get(i).rank = i + 1;
        }

        broadcast(NetworkMessage.gameOver(results));

        // Give clients a moment to receive the message before closing
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        stop();
    }

    // ------------------------------------------------------------------
    // Broadcast helper
    // ------------------------------------------------------------------
    private void sendRejection(Socket socket, String reason) {
        try {
            ObjectOutputStream rejectOut = new ObjectOutputStream(socket.getOutputStream());
            rejectOut.flush();
            rejectOut.writeObject(NetworkMessage.rejected(reason));
            rejectOut.flush();
        } catch (IOException ignored) {}
    }

    /** Sends a message to all connected clients. */
    private void broadcast(NetworkMessage msg) {
        for (ClientHandler c : clients) {
            c.send(msg);
        }
    }

    // ------------------------------------------------------------------
    // Utility
    // ------------------------------------------------------------------

    public boolean isGameStarted() { return gameStarted; }
    public int getConnectedCount()  { return clients.size(); }
}