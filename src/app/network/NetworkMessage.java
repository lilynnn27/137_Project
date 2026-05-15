package app.network;

import java.io.Serializable;
import java.util.List;

/**
 * NetworkMessage — the single shared envelope for all client↔server
 * communication.
 *
 * Every message has a {@link Type} that tells the receiver how to interpret the
 * payload fields. Only the fields relevant to each type are populated; the rest
 * stay null / 0.
 *
 * Serialized over the wire with Java's built-in ObjectInputStream /
 * ObjectOutputStream (no external library needed).
 *
 * ┌──────────────────┬──────────────┬──────────────────────────────────────┐
 * │ Type             │ Direction    │ Key payload fields                   │
 * ├──────────────────┼──────────────┼──────────────────────────────────────┤
 * │ PLAYER_JOIN      │ Client→Server│ playerName                           │
 * │ LOBBY_UPDATE     │ Server→Client│ players                              │
 * │ PLAYER_READY     │ Client→Server│ playerId                             │
 * │ START_GAME       │ Server→Client│ playerId, spawnX, spawnY, colorHex,  │
 * │                  │              │ players                              │
 * │ POSITION_UPDATE  │ Client→Server│ playerId, x, y, dirX, dirY,          │
 * │                  │              │ trailPoints                          │
 * │ GAME_STATE       │ Server→Client│ playerStates                         │
 * │ PLAYER_DIED      │ Server→Client│ playerId                             │
 * │ GAME_OVER        │ Server→Client│ results                              │
 * │ PING / PONG      │ Both         │ (empty — latency check)              │
 * └──────────────────┴──────────────┴──────────────────────────────────────┘
 */
public class NetworkMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    // ------------------------------------------------------------------
    // Message type enum
    // ------------------------------------------------------------------

    public enum Type {
        /** Client announces itself to the server. */
        PLAYER_JOIN,
        /** Server broadcasts updated lobby roster to all clients. */
        LOBBY_UPDATE,
        /** Client signals it is ready to start. */
        PLAYER_READY,
        /**
         * Server signals all clients the game is beginning.
         * Each client receives its own assigned spawn and color.
         */
        START_GAME,
        /** Client sends its latest position + trail to the server. */
        POSITION_UPDATE,
        /**
         * Server broadcasts the authoritative snapshot of all player
         * states to every client (sent every game tick).
         */
        GAME_STATE,
        /** Server notifies everyone that a specific player has died. */
        PLAYER_DIED,
        /** Server broadcasts the final leaderboard when the game ends. */
        GAME_OVER,
        /** Heartbeat — receiver should reply with PONG. */
        PING,
        /** Heartbeat reply. */
        PONG,
        /** Server → Client: connection rejected before joining (lobby full, game started, etc.). */
        REJECTED
    }

    // ------------------------------------------------------------------
    // Nested data classes (Serializable so they travel with the message)
    // ------------------------------------------------------------------

    /**
     * Snapshot of a single player's in-game state.
     * Sent inside GAME_STATE messages.
     */
    public static class PlayerState implements Serializable {
        private static final long serialVersionUID = 1L;

        public int    playerId;
        public String playerName;
        public String colorHex;   // e.g. "#FF7043"
        public double x;
        public double y;
        public double dirX;
        public double dirY;
        /** Packed trail: alternating x,y doubles — e.g. [x0,y0,x1,y1,...] */
        public double[] trailPoints;
        public boolean  isDead;
        public double   territoryPercent; // 0.0–100.0

        public PlayerState() {}

        public PlayerState(int playerId, String playerName, String colorHex,
                           double x, double y, double dirX, double dirY,
                           double[] trailPoints, boolean isDead,
                           double territoryPercent) {
            this.playerId         = playerId;
            this.playerName       = playerName;
            this.colorHex         = colorHex;
            this.x                = x;
            this.y                = y;
            this.dirX             = dirX;
            this.dirY             = dirY;
            this.trailPoints      = trailPoints;
            this.isDead           = isDead;
            this.territoryPercent = territoryPercent;
        }
    }

    /**
     * Lightweight lobby-roster entry (no position data needed yet).
     * Sent inside LOBBY_UPDATE messages.
     */
    public static class LobbyPlayer implements Serializable {
        private static final long serialVersionUID = 1L;

        public int     playerId;
        public String  playerName;
        public String  colorHex;
        public boolean isReady;

        public LobbyPlayer() {}

        public LobbyPlayer(int playerId, String playerName,
                            String colorHex, boolean isReady) {
            this.playerId   = playerId;
            this.playerName = playerName;
            this.colorHex   = colorHex;
            this.isReady    = isReady;
        }
    }

    /**
     * End-of-game result entry.
     * Sent inside GAME_OVER messages, one per player.
     */
    public static class GameResult implements Serializable {
        private static final long serialVersionUID = 1L;

        public int    playerId;
        public String playerName;
        public String colorHex;
        public double territoryPercent;
        public int    rank; // 1 = winner

        public GameResult() {}

        public GameResult(int playerId, String playerName,
                          String colorHex, double territoryPercent, int rank) {
            this.playerId         = playerId;
            this.playerName       = playerName;
            this.colorHex         = colorHex;
            this.territoryPercent = territoryPercent;
            this.rank             = rank;
        }
    }

    // ------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------

    /** Discriminator — always set. */
    public Type type;

    // --- Identity / lobby ---
    /** Assigned by the server on join; echoed back in all subsequent messages. */
    public int    playerId;
    public String playerName;
    /** CSS hex color assigned by the server (e.g. "#FF7043"). */
    public String colorHex;
    /** Human-readable reason string — used by REJECTED messages. */
    public String message;

    // --- Spawn info (START_GAME) ---
    public double spawnX;
    public double spawnY;

    // --- Movement (POSITION_UPDATE) ---
    public double   x;
    public double   y;
    public double   dirX;
    public double   dirY;
    /** Packed trail: alternating x,y doubles. */
    public double[] trailPoints;

    // --- Territory ---
    public double territoryPercent;

    // --- Collections ---
    /** Lobby roster — populated for LOBBY_UPDATE. */
    public List<LobbyPlayer>  players;
    /** All live player snapshots — populated for GAME_STATE. */
    public List<PlayerState>  playerStates;
    /** Final leaderboard — populated for GAME_OVER. */
    public List<GameResult>   results;

    // ------------------------------------------------------------------
    // Constructors (static factory helpers keep call sites readable)
    // ------------------------------------------------------------------

    public NetworkMessage() {}

    // ---- Client → Server ----

    public static NetworkMessage join(String playerName) {
        NetworkMessage m = new NetworkMessage();
        m.type       = Type.PLAYER_JOIN;
        m.playerName = playerName;
        return m;
    }

    public static NetworkMessage ready(int playerId) {
        NetworkMessage m = new NetworkMessage();
        m.type     = Type.PLAYER_READY;
        m.playerId = playerId;
        return m;
    }

    public static NetworkMessage positionUpdate(int playerId,
                                                double x, double y,
                                                double dirX, double dirY,
                                                double[] trailPoints,
                                                double territoryPercent) {
        NetworkMessage m = new NetworkMessage();
        m.type              = Type.POSITION_UPDATE;
        m.playerId          = playerId;
        m.x                 = x;
        m.y                 = y;
        m.dirX              = dirX;
        m.dirY              = dirY;
        m.trailPoints       = trailPoints;
        m.territoryPercent  = territoryPercent;
        return m;
    }

    public static NetworkMessage ping() {
        NetworkMessage m = new NetworkMessage();
        m.type = Type.PING;
        return m;
    }

    // ---- Server → Client ----

    public static NetworkMessage lobbyUpdate(List<LobbyPlayer> players) {
        NetworkMessage m = new NetworkMessage();
        m.type    = Type.LOBBY_UPDATE;
        m.players = players;
        return m;
    }

    public static NetworkMessage startGame(int playerId, double spawnX,
                                           double spawnY, String colorHex,
                                           List<LobbyPlayer> players) {
        NetworkMessage m = new NetworkMessage();
        m.type     = Type.START_GAME;
        m.playerId = playerId;
        m.spawnX   = spawnX;
        m.spawnY   = spawnY;
        m.colorHex = colorHex;
        m.players  = players;
        return m;
    }

    public static NetworkMessage gameState(List<PlayerState> playerStates) {
        NetworkMessage m = new NetworkMessage();
        m.type         = Type.GAME_STATE;
        m.playerStates = playerStates;
        return m;
    }

    public static NetworkMessage playerDied(int playerId) {
        NetworkMessage m = new NetworkMessage();
        m.type     = Type.PLAYER_DIED;
        m.playerId = playerId;
        return m;
    }

    public static NetworkMessage gameOver(List<GameResult> results) {
        NetworkMessage m = new NetworkMessage();
        m.type    = Type.GAME_OVER;
        m.results = results;
        return m;
    }

    public static NetworkMessage pong() {
        NetworkMessage m = new NetworkMessage();
        m.type = Type.PONG;
        return m;
    }

    public static NetworkMessage rejected(String text) {
        NetworkMessage m = new NetworkMessage();
        m.type    = Type.REJECTED;
        m.message = text;
        return m;
    }
}