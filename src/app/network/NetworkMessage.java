package app.network;

import java.io.Serializable;
import java.util.List;

public class NetworkMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        PLAYER_JOIN, // Client → Server: new player wants to join the lobby (includes desired name)
        LOBBY_UPDATE, // Server → Client: updated lobby roster
        PLAYER_READY, // Server → Client: game is starting, with assigned player ID, spawn position, and color
        START_GAME, // Server → Client: game is starting, with assigned player ID, spawn position, and color
        POSITION_UPDATE, // Client → Server: player's current position, direction, trail, and territory percent
        GAME_STATE, // Server → Client: snapshot of all live players' states (position, direction, trail, territory percent)
        PLAYER_DIED, // Server → Client: notification that a player has died
        GAME_OVER, // Server → Client: game over message with final results
        PING, // Client → Server: heartbeat ping to keep connection alive
        PONG, // Server → Client: response to PING
        REJECTED // Server → Client: connection rejected 
    }

    public static class PlayerState implements Serializable {
        private static final long serialVersionUID = 1L;

        public int    playerId;
        public String playerName;
        public String colorHex;   
        public double x;
        public double y;
        public double dirX;
        public double dirY;
        public double[] trailPoints;
        public boolean  isDead;
        public double   territoryPercent; 

        public PlayerState() {}

        public PlayerState(int playerId, String playerName, String colorHex,
                double x, double y, double dirX, double dirY, double[] trailPoints, boolean isDead, double territoryPercent) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.colorHex = colorHex;
            this.x = x;
            this.y = y;
            this.dirX = dirX;
            this.dirY = dirY;
            this.trailPoints = trailPoints;
            this.isDead = isDead;
            this.territoryPercent = territoryPercent;
        }
    }

    public static class LobbyPlayer implements Serializable {
        private static final long serialVersionUID = 1L;

        public int playerId;
        public String playerName;
        public String colorHex;
        public boolean isReady;

        public LobbyPlayer() {}

        public LobbyPlayer(int playerId, String playerName, String colorHex, boolean isReady) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.colorHex = colorHex;
            this.isReady = isReady;
        }
    }

    public static class GameResult implements Serializable {
        private static final long serialVersionUID = 1L;

        public int playerId;
        public String playerName;
        public String colorHex;
        public double territoryPercent;
        public int rank; 

        public GameResult() {}

        public GameResult(int playerId, String playerName, String colorHex, double territoryPercent, int rank) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.colorHex = colorHex;
            this.territoryPercent = territoryPercent;
            this.rank = rank;
        }
    }

    public Type type;

    public int    playerId;
    public String playerName;
    public String colorHex;
    public String message;

    public double spawnX;
    public double spawnY;

    public double   x;
    public double   y;
    public double   dirX;
    public double   dirY;
    public double[] trailPoints;

    public double territoryPercent;

    public List<LobbyPlayer> players;
    public List<PlayerState> playerStates;
    public List<GameResult> results;

    public NetworkMessage() {}

    public static NetworkMessage join(String playerName) {
        NetworkMessage m = new NetworkMessage();
        m.type = Type.PLAYER_JOIN;
        m.playerName = playerName;
        return m;
    }

    public static NetworkMessage ready(int playerId) {
        NetworkMessage m = new NetworkMessage();
        m.type = Type.PLAYER_READY;
        m.playerId = playerId;
        return m;
    }

    public static NetworkMessage positionUpdate(int playerId, double x, double y, double dirX, double dirY, double[] trailPoints, double territoryPercent) {
        NetworkMessage m = new NetworkMessage();
        m.type = Type.POSITION_UPDATE;
        m.playerId = playerId;
        m.x = x;
        m.y = y;
        m.dirX = dirX;
        m.dirY = dirY;
        m.trailPoints = trailPoints;
        m.territoryPercent = territoryPercent;
        return m;
    }

    public static NetworkMessage ping() {
        NetworkMessage m = new NetworkMessage();
        m.type = Type.PING;
        return m;
    }

    public static NetworkMessage lobbyUpdate(List<LobbyPlayer> players) {
        NetworkMessage m = new NetworkMessage();
        m.type = Type.LOBBY_UPDATE;
        m.players = players;
        return m;
    }

    public static NetworkMessage startGame(int playerId, double spawnX, double spawnY, String colorHex, List<LobbyPlayer> players) {
        NetworkMessage m = new NetworkMessage();
        m.type = Type.START_GAME;
        m.playerId = playerId;
        m.spawnX = spawnX;
        m.spawnY = spawnY;
        m.colorHex = colorHex;
        m.players = players;
        return m;
    }

    public static NetworkMessage gameState(List<PlayerState> playerStates) {
        NetworkMessage m = new NetworkMessage();
        m.type = Type.GAME_STATE;
        m.playerStates = playerStates;
        return m;
    }

    public static NetworkMessage playerDied(int playerId) {
        NetworkMessage m = new NetworkMessage();
        m.type = Type.PLAYER_DIED;
        m.playerId = playerId;
        return m;
    }

    public static NetworkMessage gameOver(List<GameResult> results) {
        NetworkMessage m = new NetworkMessage();
        m.type = Type.GAME_OVER;
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
        m.type = Type.REJECTED;
        m.message = text;
        return m;
    }
}