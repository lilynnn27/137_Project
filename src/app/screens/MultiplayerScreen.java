package app.screens;

import app.Main;
import app.network.GameClient;
import app.network.GameServer;
import app.network.NetworkMessage;
import app.network.NetworkMessage.LobbyPlayer;
import app.utils.UIUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * MultiplayerScreen — the networked lobby.
 *
 * Two modes selectable by the player:
 *   HOST   — starts a {@link GameServer} on this machine, then connects as
 *             the first client.
 *   JOIN   — connects to a server already running on another machine.
 *
 * Once connected the lobby shows live player slots populated by LOBBY_UPDATE
 * messages from the server.  When the server sends START_GAME, the screen
 * transitions to the multiplayer gameplay screen.
 */
public class MultiplayerScreen {

    // ------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------

    private static final String[] DOUGH_FILES = {
        "orange", "blue", "green", "red", "yellow", "pink", "purple", "indigo"
    };

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    private final VBox  root;
    private final Main  mainApp;

    private GameClient  client;
    private Thread      serverThread;

    private VBox        playerList;
    private Label       statusLabel;
    private Button      readyBtn;
    private TextField   nameField;
    private TextField   ipField;

    private boolean     isReady     = false;
    private boolean     isConnected = false;

    private static final String NORMAL_STYLE =
        "-fx-background-color: transparent; -fx-text-fill: white; " +
        "-fx-border-color: white; -fx-border-width: 2px; " +
        "-fx-padding: 15 40; -fx-cursor: hand;";
    private static final String READY_STYLE =
        "-fx-background-color: #4CAF50; -fx-text-fill: white; " +
        "-fx-border-color: #4CAF50; -fx-border-width: 2px; " +
        "-fx-padding: 15 40; -fx-cursor: hand;";
    private static final String DISABLED_STYLE =
        "-fx-background-color: transparent; -fx-text-fill: #555555; " +
        "-fx-border-color: #555555; -fx-border-width: 2px; " +
        "-fx-padding: 15 40;";

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    public MultiplayerScreen(Main mainApp) {
        this.mainApp = mainApp;

        // Title
        Label title = new Label("The Tray");
        title.setFont(Font.font(UIUtils.MAIN_FONT, 72));
        title.setStyle("-fx-text-fill: #b89664;");

        Label subtitle = new Label("Lobby");
        subtitle.setFont(Font.font(UIUtils.MAIN_FONT, 28));
        subtitle.setStyle("-fx-text-fill: #AAAAAA;");

        VBox titleBox = new VBox(4, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);

        // Connection inputs
        nameField = new TextField("Player");
        nameField.setPromptText("Your name");
        nameField.setMaxWidth(180);
        nameField.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white; " +
                           "-fx-border-color: #444; -fx-border-radius: 4; -fx-padding: 6 10;");

        ipField = new TextField("localhost");
        ipField.setPromptText("Server IP (for Join)");
        ipField.setMaxWidth(180);
        ipField.setStyle("-fx-background-color: #1c1c1c; -fx-text-fill: white; " +
                         "-fx-border-color: #444; -fx-border-radius: 4; -fx-padding: 6 10;");

        Button hostBtn = new Button("Host Game");
        UIUtils.styleButton(hostBtn, UIUtils.BUTTON_STYLE, UIUtils.BUTTON_HOVER_STYLE);
        hostBtn.setOnAction(e -> hostGame());

        Button joinBtn = new Button("Join Game");
        UIUtils.styleButton(joinBtn, UIUtils.BUTTON_STYLE, UIUtils.BUTTON_HOVER_STYLE);
        joinBtn.setOnAction(e -> joinGame());

        HBox connectionRow = new HBox(16, nameField, ipField, hostBtn, joinBtn);
        connectionRow.setAlignment(Pos.CENTER);

        // Player list panel
        playerList = new VBox(8);
        playerList.setAlignment(Pos.CENTER_LEFT);
        playerList.setPadding(new Insets(20));
        playerList.setStyle("-fx-background-color: #111111; -fx-background-radius: 12;");

        Label listHeader = new Label("Players");
        listHeader.setFont(Font.font(UIUtils.MAIN_FONT, 26));
        listHeader.setStyle("-fx-text-fill: white;");
        VBox.setMargin(listHeader, new Insets(0, 0, 8, 0));
        playerList.getChildren().add(listHeader);

        for (int i = 0; i < 4; i++) {
            playerList.getChildren().add(buildEmptySlot(i));
        }

        StackPane arenaPreview = buildArenaPreview();

        HBox center = new HBox(40, playerList, arenaPreview);
        center.setAlignment(Pos.CENTER);

        // Status label
        statusLabel = new Label("Host or join a lobby to begin…");
        statusLabel.setFont(Font.font(UIUtils.MAIN_FONT, 24));
        statusLabel.setStyle("-fx-text-fill: #AAAAAA;");

        // Ready button (disabled until connected)
        readyBtn = new Button("Ready");
        readyBtn.setFont(Font.font(UIUtils.MAIN_FONT, 26));
        readyBtn.setStyle(DISABLED_STYLE);
        readyBtn.setDisable(true);
        readyBtn.setOnMouseEntered(e -> { if (!isReady && isConnected) readyBtn.setStyle(UIUtils.BUTTON_HOVER_STYLE); });
        readyBtn.setOnMouseExited(e ->  { if (!isReady && isConnected) readyBtn.setStyle(NORMAL_STYLE); });
        readyBtn.setOnAction(e -> toggleReady());

        Button backBtn = new Button("Back to Menu");
        UIUtils.styleButton(backBtn, UIUtils.BUTTON_STYLE, UIUtils.BUTTON_HOVER_STYLE);
        backBtn.setOnAction(e -> {
            cleanup();
            mainApp.showLandingPage();
        });

        HBox buttons = new HBox(30, readyBtn, backBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox layout = new VBox(20, titleBox, connectionRow, center, statusLabel, buttons);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle(UIUtils.BG_STYLE);
        layout.setPadding(new Insets(40));
        this.root = layout;
    }

    // ------------------------------------------------------------------
    // Networking actions
    // ------------------------------------------------------------------

    private void hostGame() {
        GameServer server = new GameServer(GameServer.DEFAULT_PORT);
        serverThread = new Thread(server::start, "GameServer");
        serverThread.setDaemon(true);
        serverThread.start();

        new Thread(() -> {
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                ipField.setText("localhost");
                joinGame();
            });
        }).start();

        setStatus("Server started — waiting for players…", "#4CAF50");
    }

    private void joinGame() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = "Player";
        String ip = ipField.getText().trim();
        if (ip.isEmpty()) ip = "localhost";

        setStatus("Connecting to " + ip + "…", "#FFA726");

        final String finalName = name;
        final String finalIp   = ip;

        new Thread(() -> {
            try {
                client = new GameClient(finalIp, GameServer.DEFAULT_PORT, finalName)
                    .onLobbyUpdate(players -> Platform.runLater(() -> applyLobbyUpdate(players)))
                    .onStartGame(msg       -> Platform.runLater(() -> startGame(msg)))
                    .onGameState(states    -> { /* lobby ignores game state */ })
                    .onGameOver(results    -> { /* lobby ignores game over */ })
                    .onError(err           -> Platform.runLater(() ->
                        setStatus("Error: " + err, "#F44336")));

                client.connect();

                Platform.runLater(() -> {
                    isConnected = true;
                    readyBtn.setDisable(false);
                    readyBtn.setStyle(NORMAL_STYLE);
                    setStatus("Connected! Click Ready when you're set.", "#4CAF50");
                });

            } catch (IOException e) {
                Platform.runLater(() ->
                    setStatus("Could not connect to " + finalIp + " — is the server running?", "#F44336"));
            }
        }, "ConnectThread").start();
    }

    private void toggleReady() {
        if (!isConnected || client == null) return;
        isReady = !isReady;
        client.sendReady();
        readyBtn.setStyle(isReady ? READY_STYLE : NORMAL_STYLE);
        readyBtn.setText(isReady ? "Ready!" : "Ready");
        setStatus(isReady ? "Waiting for others…" : "Click Ready when you're set.", "#AAAAAA");
    }

    // ------------------------------------------------------------------
    // Server message handlers (JavaFX thread)
    // ------------------------------------------------------------------

    private void applyLobbyUpdate(List<LobbyPlayer> players) {
        playerList.getChildren().subList(1, playerList.getChildren().size()).clear();

        int slots = Math.max(GameServer.MIN_PLAYERS, players.size());
        for (int i = 0; i < slots; i++) {
            if (i < players.size()) {
                playerList.getChildren().add(buildFilledSlot(i, players.get(i)));
            } else {
                playerList.getChildren().add(buildEmptySlot(i));
            }
        }

        long readyCount = players.stream().filter(p -> p.isReady).count();
        setStatus("Players: " + players.size() + "/" + GameServer.MIN_PLAYERS
                  + "  |  Ready: " + readyCount + "/" + players.size(), "#AAAAAA");
    }

    private void startGame(NetworkMessage msg) {
        mainApp.showMultiplayerGame(client, msg.spawnX, msg.spawnY, msg.colorHex, msg.playerId);
    }

    // ------------------------------------------------------------------
    // Slot builders
    // ------------------------------------------------------------------

    private HBox buildFilledSlot(int index, LobbyPlayer player) {
        HBox slot = slotBase(index);

        String doughFile = DOUGH_FILES[Math.abs(player.colorHex.hashCode()) % DOUGH_FILES.length];
        File   imgFile   = new File("assets/images/PlayersDough/" + doughFile + ".png");
        ImageView icon   = new ImageView();
        if (imgFile.exists()) {
            icon.setImage(new Image(imgFile.toURI().toString()));
        }
        icon.setFitWidth(44);
        icon.setFitHeight(44);
        icon.setPreserveRatio(true);

        Label name = new Label(player.playerName + (player.isReady ? " ✓" : ""));
        name.setFont(Font.font(UIUtils.MAIN_FONT, 22));
        name.setStyle("-fx-text-fill: " + player.colorHex + ";");

        slot.getChildren().addAll(icon, name);
        return slot;
    }

    private HBox buildEmptySlot(int index) {
        HBox slot = slotBase(index);

        Circle placeholder = new Circle(22);
        placeholder.setFill(Color.web("#2a2a2a"));
        placeholder.setStroke(Color.web("#444444"));
        placeholder.setStrokeWidth(2);

        Label name = new Label("Waiting…");
        name.setFont(Font.font(UIUtils.MAIN_FONT, 22));
        name.setStyle("-fx-text-fill: #444444;");

        slot.getChildren().addAll(placeholder, name);
        return slot;
    }

    private HBox slotBase(int index) {
        HBox slot = new HBox(14);
        slot.setAlignment(Pos.CENTER_LEFT);
        slot.setPadding(new Insets(10, 18, 10, 18));
        slot.setPrefWidth(300);
        slot.setStyle("-fx-background-color: " + (index % 2 == 0 ? "#1c1c1c" : "#141414")
                    + "; -fx-background-radius: 8;");
        return slot;
    }

    private StackPane buildArenaPreview() {
        StackPane preview = new StackPane();
        preview.setPrefSize(300, 300);
        preview.setMaxSize(300, 300);

        File bgFile = new File("assets/images/GameplayBackground.jpg");
        if (bgFile.exists()) {
            ImageView bg = new ImageView(new Image(bgFile.toURI().toString()));
            bg.setFitWidth(300);
            bg.setFitHeight(300);
            bg.setPreserveRatio(false);
            bg.setClip(new Circle(150, 150, 145));
            preview.getChildren().add(bg);
        }

        Circle border = new Circle(145);
        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.web("#b89664"));
        border.setStrokeWidth(3);
        preview.getChildren().add(border);

        Label previewLabel = new Label("Arena Preview");
        previewLabel.setFont(Font.font(UIUtils.MAIN_FONT, 16));
        previewLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5);");
        StackPane.setAlignment(previewLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(previewLabel, new Insets(0, 0, 14, 0));
        preview.getChildren().add(previewLabel);

        return preview;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void setStatus(String text, String hexColor) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-size: 24px;");
    }

    private void cleanup() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
    }

    public javafx.scene.Parent getRoot() {
        return root;
    }
}