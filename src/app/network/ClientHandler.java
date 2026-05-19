package app.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameServer server;
    private ObjectOutputStream out;
    private ObjectInputStream  in;

    // Assigned by GameServer after PLAYER_JOIN is received.
    private int playerId = -1;
    private String playerName = "Unknown";

    // False once the socket closes or a fatal error occurs. 
    private volatile boolean running = true;

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------
    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }

    // ------------------------------------------------------------------
    // Runnable — message read loop
    // ------------------------------------------------------------------
    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("[Server] Client connected: " + socket.getInetAddress());

            while (running) {
                Object obj = in.readObject();
                if (obj instanceof NetworkMessage msg) {
                    handleMessage(msg);
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            if (running) {
                System.out.println("[Server] Client " + playerId + " disconnected: " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    // ------------------------------------------------------------------
    // Message dispatch
    // ------------------------------------------------------------------

    private void handleMessage(NetworkMessage msg) {
        switch (msg.type) {
            case PLAYER_JOIN -> {
                // Server assigns the playerId; the name comes from the client
                this.playerName = msg.playerName != null ? msg.playerName : "Player";
                server.onPlayerJoin(this);
            }
            case PLAYER_READY -> {
                server.onPlayerReady(this);
            }
            case POSITION_UPDATE -> {
                server.onPositionUpdate(this, msg);
            }
            case PING -> {
                send(NetworkMessage.pong());
            }
            case CHAT -> {
                server.onChat(this, msg);
            }
            default -> {
                System.out.println("[Server] Unexpected message type from client " + playerId + ": " + msg.type);
            }
        }
    }

    // ------------------------------------------------------------------
    // Outbound — called by GameServer on any thread
    // ------------------------------------------------------------------
    public synchronized void send(NetworkMessage msg) {
        if (!running || out == null) return;
        try {
            out.writeObject(msg);
            out.flush();
            // Reset the stream to prevent object-graph caching, which would
            // cause the receiver to see stale field values on repeated sends
            // of the same logical message type.
            out.reset();
        } catch (IOException e) {
            System.out.println("[Server] Failed to send to client " + playerId + ": " + e.getMessage());
            disconnect();
        }
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    public void disconnect() {
        if (!running) return; // already disconnected
        running = false;
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { socket.close(); }              catch (IOException ignored) {}
        server.onClientDisconnected(this);
    }

    // ------------------------------------------------------------------
    // Getters / setters (called by GameServer)
    // ------------------------------------------------------------------
    public int    getPlayerId()   { return playerId; }
    public String getPlayerName() { return playerName; }

    /** Called by GameServer immediately after it assigns an ID. */
    public void setPlayerId(int id) { this.playerId = id; }
}
