package app.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

import app.network.NetworkMessage.GameResult;
import app.network.NetworkMessage.LobbyPlayer;
import app.network.NetworkMessage.PlayerState;

public class GameClient {
    private final String host;
    private final int port;
    private final String playerName;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;

    // Assigned by the server after PLAYER_JOIN is acknowledged.
    private volatile int myPlayerId = -1;
    private volatile boolean running = false;

    // ------------------------------------------------------------------
    // Callbacks (set before calling connect())
    // ------------------------------------------------------------------

    // Called when the server broadcasts an updated lobby roster. 
    private Consumer<List<LobbyPlayer>> onLobbyUpdate;

    // Called when the server sends START_GAME (carries spawn + color). 
    private Consumer<NetworkMessage> onStartGame;

    // Called every server tick with all players' latest states. 
    private Consumer<List<PlayerState>> onGameState;

    // Called when a specific player dies. 
    private Consumer<Integer> onPlayerDied;

    // Called when the server sends the final GAME_OVER leaderboard. 
    private Consumer<List<GameResult>> onGameOver;

    // Called when the server rejects the connection before the lobby is entered. 
    private Consumer<String> onRejected;

    // Called on any connection error so the UI can show a message. 
    private Consumer<String> onError;

    // Called when a chat message is received.
    private Consumer<NetworkMessage> onChat;

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    public GameClient(String host, int port, String playerName) {
        this.host = host;
        this.port = port;
        this.playerName = playerName;
    }

    // ------------------------------------------------------------------
    // Callback setters (fluent)
    // ------------------------------------------------------------------

    public GameClient onLobbyUpdate(Consumer<List<LobbyPlayer>> cb) {
        this.onLobbyUpdate = cb; return this;
    }

    public GameClient onStartGame(Consumer<NetworkMessage> cb) {
        this.onStartGame = cb; return this;
    }

    public GameClient onGameState(Consumer<List<PlayerState>> cb) {
        this.onGameState = cb; return this;
    }

    public GameClient onPlayerDied(Consumer<Integer> cb) {
        this.onPlayerDied = cb; return this;
    }

    public GameClient onGameOver(Consumer<List<GameResult>> cb) {
        this.onGameOver = cb; return this;
    }

    public GameClient onRejected(Consumer<String> cb) {
        this.onRejected = cb; return this;
    }

    public GameClient onError(Consumer<String> cb) {
        this.onError = cb; return this;
    }

    public GameClient onChat(Consumer<NetworkMessage> cb) {
        this.onChat = cb; return this;
    }

    // ------------------------------------------------------------------
    // Connect - open socket and start receive loop (TCP)
    // ------------------------------------------------------------------

    public void connect() throws IOException {
        socket = new Socket(host, port);

        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in  = new ObjectInputStream(socket.getInputStream());

        running = true;
        System.out.println("[Client] Connected to " + host + ":" + port);

        Thread receiveThread = new Thread(this::receiveLoop, "GameClient-Recv");
        receiveThread.setDaemon(true);
        receiveThread.start();

        send(NetworkMessage.join(playerName));
    }

    // ------------------------------------------------------------------
    // Receive loop
    // ------------------------------------------------------------------
    private void receiveLoop() {
        try {
            while (true) {
                Object obj = in.readObject();
                if (obj instanceof NetworkMessage msg) {
                    dispatch(msg);
                }
                if (!running) break;
            }
        } catch (IOException | ClassNotFoundException e) {
            if (running) {
                String error = "Connection lost: " + e.getMessage();
                System.out.println("[Client] " + error);
                if (onError != null) onError.accept(error);
            }
        } finally {
            disconnect();
        }
    }

    private void dispatch(NetworkMessage msg) {
        switch (msg.type) {
            case LOBBY_UPDATE -> {
                // Server embeds our playerId in every lobby update
                if (msg.playerId > 0) myPlayerId = msg.playerId;
                if (onLobbyUpdate != null && msg.players != null) {
                    onLobbyUpdate.accept(msg.players);
                }
            }
            case START_GAME -> {
                myPlayerId = msg.playerId;
                if (onStartGame != null) onStartGame.accept(msg);
            }
            case GAME_STATE -> {
                if (onGameState != null && msg.playerStates != null) {
                    onGameState.accept(msg.playerStates);
                }
            }
            case PLAYER_DIED -> {
                if (onPlayerDied != null) onPlayerDied.accept(msg.playerId);
            }
            case GAME_OVER -> {
                if (onGameOver != null && msg.results != null) {
                    onGameOver.accept(msg.results);
                }
            }
            case REJECTED -> {
                System.out.println("[Client] Rejected by server: " + msg.message);
                if (onRejected != null) onRejected.accept(msg.message);
                disconnect();
            }
            case PING -> {
                send(NetworkMessage.pong());
            }
            case CHAT -> {
                if (onChat != null) onChat.accept(msg);
            }
            default -> {
                System.out.println("[Client] Unexpected message: " + msg.type);
            }
        }
    }

    // ------------------------------------------------------------------
    // Outbound helpers
    // ------------------------------------------------------------------
    public void sendReady() {
        send(NetworkMessage.ready(myPlayerId));
    }

    public void sendChat(String message) {
        send(NetworkMessage.chat(playerName, message));
    }

    public void sendPositionUpdate(double x, double y, double dirX, double dirY, double[] trailPoints, double territoryPercent) {
        send(NetworkMessage.positionUpdate(
            myPlayerId, x, y, dirX, dirY, trailPoints, territoryPercent
        ));
    }

    public synchronized void send(NetworkMessage msg) {
        if (!running || out == null) return;
        try {
            out.writeObject(msg);
            out.flush();
            out.reset(); // prevent stale cached objects being re-sent
        } catch (IOException e) {
            System.out.println("[Client] Send failed: " + e.getMessage());
            disconnect();
        }
    }

    // ------------------------------------------------------------------
    // Disconnect
    // ------------------------------------------------------------------

    public void disconnect() {
        if (!running) return;
        running = false;
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        System.out.println("[Client] Disconnected.");
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public int     getMyPlayerId() { return myPlayerId; }
    public boolean isConnected()   { return running; }
    public String  getPlayerName() { return playerName; }
}
