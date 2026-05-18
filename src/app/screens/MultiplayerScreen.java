package app.screens;

import java.io.IOException;
import java.util.List;

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
import javafx.scene.layout.FlowPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

public class MultiplayerScreen {
    private static final String[] DOUGH_FILES = {
            "orange", "blue", "green", "red", "yellow", "pink", "purple", "indigo"
    };

    // Warm kitchen color palette
    private static final String CREAM = "#f5e6c8";
    private static final String BROWN = "#3d282e";
    private static final String ORANGE = "#c46a2d";
    private static final String GOLD = "#b89664";
    private static final String MUTED = "#888888";

    // Font constants — loaded once at class level, applied at construction, never re-set
    private static final Font FONT_TITLE    = Font.font(UIUtils.MAIN_FONT, 72);
    private static final Font FONT_SUBTITLE = Font.font(UIUtils.MAIN_FONT, 28);
    private static final Font FONT_HEADER   = Font.font(UIUtils.MAIN_FONT, 26);
    private static final Font FONT_STATUS   = Font.font(UIUtils.MAIN_FONT, 24);
    private static final Font FONT_BODY     = Font.font(UIUtils.MAIN_FONT, 22);
    private static final Font FONT_BUTTON   = Font.font(UIUtils.MAIN_FONT, 20);
    private static final Font FONT_SMALL    = Font.font(UIUtils.MAIN_FONT, 18);
    private static final Font FONT_FIELD    = Font.font(UIUtils.MAIN_FONT, 16);

    // Ready button states
    private static final String NORMAL_STYLE = "-fx-background-color: transparent; -fx-text-fill: " + CREAM + "; " +
            "-fx-border-color: " + CREAM + "; -fx-border-width: 2px; " +
            "-fx-padding: 15 40; -fx-cursor: hand; -fx-font-weight: bold; " +
            "-fx-font-family: '" + UIUtils.MAIN_FONT + "'; -fx-font-size: 26px;";
    private static final String NORMAL_STYLE_HOVER = "-fx-background-color: " + CREAM + "; -fx-text-fill: " + BROWN + "; " +
            "-fx-border-color: " + CREAM + "; -fx-border-width: 2px; " +
            "-fx-padding: 15 40; -fx-cursor: hand; -fx-font-weight: bold; " +
            "-fx-font-family: '" + UIUtils.MAIN_FONT + "'; -fx-font-size: 26px;";
    private static final String READY_STYLE = "-fx-background-color: #4CAF50; -fx-text-fill: white; " +
            "-fx-border-color: #4CAF50; -fx-border-width: 2px; " +
            "-fx-padding: 15 40; -fx-cursor: hand; -fx-font-weight: bold; " +
            "-fx-font-family: '" + UIUtils.MAIN_FONT + "'; -fx-font-size: 26px;";
    private static final String DISABLED_STYLE = "-fx-background-color: transparent; -fx-text-fill: #555555; " +
            "-fx-border-color: #555555; -fx-border-width: 2px; " +
            "-fx-padding: 15 40; -fx-font-weight: bold; " +
            "-fx-font-family: '" + UIUtils.MAIN_FONT + "'; -fx-font-size: 26px;";

    // General action buttons (Host, Join)
    private static final String BTN_NORMAL = "-fx-background-color: " + ORANGE + "; -fx-text-fill: " + CREAM + "; " +
            "-fx-border-color: " + ORANGE + "; -fx-border-width: 2px; " +
            "-fx-padding: 12 30; -fx-cursor: hand; -fx-font-weight: bold; " +
            "-fx-font-family: '" + UIUtils.MAIN_FONT + "'; -fx-font-size: 20px;";
    private static final String BTN_HOVER = "-fx-background-color: " + CREAM + "; -fx-text-fill: " + BROWN + "; " +
            "-fx-border-color: " + ORANGE + "; -fx-border-width: 2px; " +
            "-fx-padding: 12 30; -fx-cursor: hand; -fx-font-weight: bold; " +
            "-fx-font-family: '" + UIUtils.MAIN_FONT + "'; -fx-font-size: 20px;";

    // Secondary button (Back to Menu)
    private static final String BTN_SEC_N = "-fx-background-color: transparent; -fx-text-fill: " + CREAM + "; " +
            "-fx-border-color: " + CREAM + "; -fx-border-width: 2px; " +
            "-fx-padding: 12 30; -fx-cursor: hand; -fx-font-weight: bold; " +
            "-fx-font-family: '" + UIUtils.MAIN_FONT + "'; -fx-font-size: 20px;";
    private static final String BTN_SEC_H = "-fx-background-color: " + CREAM + "; -fx-text-fill: " + BROWN + "; " +
            "-fx-border-color: " + CREAM + "; -fx-border-width: 2px; " +
            "-fx-padding: 12 30; -fx-cursor: hand; -fx-font-weight: bold; " +
            "-fx-font-family: '" + UIUtils.MAIN_FONT + "'; -fx-font-size: 20px;";

    private void styleLocalButton(Button btn) {
        btn.setStyle(BTN_NORMAL);
        btn.setOnMouseEntered(e -> btn.setStyle(BTN_HOVER));
        btn.setOnMouseExited(e -> btn.setStyle(BTN_NORMAL));
    }

    private final StackPane root;
    private final VBox mainLayout;
    private final Main mainApp;

    private GameClient client;
    private GameServer gameServer;
    private Thread serverThread;

    private FlowPane playerList;
    private Label statusLabel;
    private Button readyBtn;
    private Button hostBtn;
    private Button joinBtn;
    private TextField nameField;
    private TextField ipField;
    private String currentServerIp = "";

    private boolean isReady = false;
    private boolean isConnected = false;

    private Image bgImage;
    private Image arenaPreviewBgImage;
    private final Image[] doughImages = new Image[DOUGH_FILES.length]; // all 8 colors
    private final Image[] previewImages = new Image[6]; // 3 hazards + 3 powerups

    public MultiplayerScreen(Main mainApp) {
        this.mainApp = mainApp;

        bgImage = UIUtils.ImageCache.get("assets/images/MainBackground.jpg");

        for (int i = 0; i < DOUGH_FILES.length; i++) {
            doughImages[i] = UIUtils.ImageCache.get("assets/images/PlayersDough/" + DOUGH_FILES[i] + ".png");
        }

        arenaPreviewBgImage = UIUtils.ImageCache.get("assets/images/GameplayBackground.jpg");

        String[] pvPaths = {
                "assets/images/hazard/RollingPin-Hazard.png",
                "assets/images/hazard/Ice-Hazard.png",
                "assets/images/hazard/RottenEgg-Hazard.png",
                "assets/images/powerup/Oil-Powerup.png",
                "assets/images/powerup/Dough-Powerup.png",
                "assets/images/powerup/Flour-Powerup.png"
        };
        for (int i = 0; i < pvPaths.length; i++) {
            previewImages[i] = UIUtils.ImageCache.get(pvPaths[i]);
        }

        // Title
        Label title = new Label("The Tray");
        title.setFont(FONT_TITLE);
        title.setStyle("-fx-text-fill: " + GOLD + "; -fx-font-weight: bold;");

        Label subtitle = new Label("Lobby");
        subtitle.setFont(FONT_SUBTITLE);
        subtitle.setStyle("-fx-text-fill: " + CREAM + ";");

        VBox titleBox = new VBox(4, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);

        // Connection inputs
        String fieldStyle = "-fx-background-color: rgba(20,15,18,0.7); -fx-text-fill: " + CREAM + "; " +
                "-fx-prompt-text-fill: #888888; " +
                "-fx-border-color: " + GOLD + "; -fx-border-radius: 4; -fx-padding: 6 10; " +
                "-fx-font-weight: bold; -fx-font-family: '" + UIUtils.MAIN_FONT + "'; -fx-font-size: 16px;";

        nameField = new TextField("Player");
        nameField.setPromptText("Your name");
        nameField.setMaxWidth(180);
        nameField.setFont(FONT_FIELD);
        nameField.setStyle(fieldStyle);

        ipField = new TextField("");
        ipField.setPromptText("Server IP or Code");
        ipField.setMaxWidth(180);
        ipField.setFont(FONT_FIELD);
        ipField.setStyle(fieldStyle);

        hostBtn = new Button("Host Game");
        styleLocalButton(hostBtn);
        hostBtn.setOnAction(e -> hostGame());

        joinBtn = new Button("Join Game");
        styleLocalButton(joinBtn);
        joinBtn.setOnAction(e -> joinGame());

        HBox connectionRow = new HBox(16, nameField, ipField, hostBtn, joinBtn);
        connectionRow.setAlignment(Pos.CENTER);

        // Player list panel — card grid, no scroll
        Label listHeader = new Label("Players");
        listHeader.setFont(FONT_HEADER);
        listHeader.setStyle("-fx-text-fill: " + GOLD + "; -fx-font-weight: bold;");

        playerList = new FlowPane(14, 14);
        playerList.setAlignment(Pos.CENTER);
        playerList.setPrefWrapLength(380);

        for (int i = 0; i < 4; i++) {
            playerList.getChildren().add(buildEmptySlot(i));
        }

        VBox playerSection = new VBox(14, listHeader, playerList);
        playerSection.setAlignment(Pos.CENTER);
        playerSection.setPadding(new Insets(20));
        playerSection.setStyle(
                "-fx-background-color: rgba(15,10,12,0.82); -fx-background-radius: 16;");

        StackPane arenaPreview = buildArenaPreview();

        HBox center = new HBox(40, playerSection, arenaPreview);
        center.setAlignment(Pos.CENTER);

        // Status label
        statusLabel = new Label("Host or join a lobby to begin…");
        statusLabel.setFont(FONT_STATUS);
        statusLabel.setStyle("-fx-text-fill: " + MUTED + ";");

        // Ready button (disabled until connected)
        readyBtn = new Button("Ready");
        readyBtn.setStyle(DISABLED_STYLE);
        readyBtn.setDisable(true);
        readyBtn.setOnMouseEntered(e -> {
            if (!isReady && isConnected)
                readyBtn.setStyle(NORMAL_STYLE_HOVER);
        });
        readyBtn.setOnMouseExited(e -> {
            if (!isReady && isConnected)
                readyBtn.setStyle(NORMAL_STYLE);
        });
        readyBtn.setOnAction(e -> toggleReady());

        Button backBtn = new Button("Back to Menu");
        backBtn.setStyle(BTN_SEC_N);
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(BTN_SEC_H));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(BTN_SEC_N));
        backBtn.setOnAction(e -> {
            cleanup();
            mainApp.showLandingPage();
        });

        HBox buttons = new HBox(30, readyBtn, backBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox layout = new VBox(20, titleBox, connectionRow, center, statusLabel, buttons, buildPreviewStrip());
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));
        this.mainLayout = layout;

        // Root with MainBackground + dark overlay
        this.root = new StackPane();

        if (bgImage != null && !bgImage.isError()) {
            ImageView bgImgView = new ImageView(bgImage);
            bgImgView.setPreserveRatio(false);
            bgImgView.fitWidthProperty().bind(this.root.widthProperty());
            bgImgView.fitHeightProperty().bind(this.root.heightProperty());
            this.root.getChildren().add(bgImgView);
        }

        Rectangle overlay = new Rectangle();
        overlay.widthProperty().bind(this.root.widthProperty());
        overlay.heightProperty().bind(this.root.heightProperty());
        overlay.setFill(Color.web("#000000", 0.62));
        this.root.getChildren().addAll(overlay, this.mainLayout);
    }

    // ------------------------------------------------------------------
    // Networking actions
    // ------------------------------------------------------------------

    private void hostGame() {
        if (!GameServer.isServerRunning()) {
            gameServer = new GameServer(GameServer.DEFAULT_PORT);
            serverThread = new Thread(gameServer::start, "GameServer");
            serverThread.setDaemon(true);
            serverThread.start();

            String ipAddress = "127.0.0.1";
            try {
                java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface
                        .getNetworkInterfaces();
                String fallbackIp = null;
                while (interfaces.hasMoreElements()) {
                    java.net.NetworkInterface iface = interfaces.nextElement();
                    if (iface.isLoopback() || !iface.isUp() || iface.isVirtual())
                        continue;

                    String dispName = iface.getDisplayName() != null ? iface.getDisplayName().toLowerCase() : "";
                    String name = iface.getName() != null ? iface.getName().toLowerCase() : "";

                    // Skip common virtual/WSL/Hyper-V interfaces that might not be caught by isVirtual()
                    if (dispName.contains("wsl") || dispName.contains("virtual") || dispName.contains("hyper-v") ||
                        name.contains("wsl") || name.contains("virtual") || name.contains("hyper-v")) {
                        continue;
                    }

                    boolean isWiFi = dispName.contains("wi-fi") || dispName.contains("wireless") || dispName.contains("wlan") ||
                                     name.contains("wi-fi") || name.contains("wireless") || name.contains("wlan");

                    java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        java.net.InetAddress addr = addresses.nextElement();
                        if (addr instanceof java.net.Inet4Address) {
                            String hostAddr = addr.getHostAddress();
                            if (isWiFi) {
                                ipAddress = hostAddr;
                                break;
                            }
                            if (fallbackIp == null && (hostAddr.startsWith("192.168.") || hostAddr.startsWith("10.")
                                    || hostAddr.startsWith("172."))) {
                                fallbackIp = hostAddr;
                            }
                        }
                    }
                    if (isWiFi && !ipAddress.equals("127.0.0.1")) {
                        break; 
                    }
                }
                if (ipAddress.equals("127.0.0.1") && fallbackIp != null) {
                    ipAddress = fallbackIp;
                }
            } catch (Exception e) {
                // fallback
            }
            currentServerIp = ipAddress;
            setStatus("Server started at " + currentServerIp + " — waiting for players…", "#4CAF50");
            hostBtn.setDisable(true);
        } else {
            setStatus("Server already running — connecting…", "#FFA726");
            hostBtn.setDisable(true);
        }

        new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
            }
            Platform.runLater(() -> {
                ipField.setText("localhost");
                joinGame();
            });
        }).start();
    }

    private void joinGame() {
        if (isConnected)
            return;

        String name = nameField.getText().trim();
        if (name.isEmpty())
            name = "Player";
        String ip = ipField.getText().trim();
        if (ip.isEmpty())
            ip = "localhost";

        // If the user entered a code instead of an IP address
        if (!ip.contains(".") && !ip.equalsIgnoreCase("localhost")) {
            ip = decodeIp(ip);
        }

        // If we are hosting, keep the LAN IP we already found
        if (!GameServer.isServerRunning()) {
            currentServerIp = ip;
        }
        setStatus("Connecting to " + ip + "…", "#FFA726");

        final String finalName = name;
        final String finalIp = ip;

        new Thread(() -> {
            try {
                client = new GameClient(finalIp, GameServer.DEFAULT_PORT, finalName)
                        .onLobbyUpdate(players -> Platform.runLater(() -> applyLobbyUpdate(players)))
                        .onStartGame(msg -> Platform.runLater(() -> startGame(msg)))
                        .onGameState(states -> {
                            /* lobby ignores game state */ })
                        .onGameOver(results -> {
                            /* lobby ignores game over */ })
                        .onRejected(msg -> Platform.runLater(() -> setStatus(msg, "#F44336")))
                        .onError(err -> Platform.runLater(() -> setStatus("Error: " + err, "#F44336")));

                client.connect();

                Platform.runLater(() -> {
                    isConnected = true;
                    joinBtn.setDisable(true);
                    readyBtn.setDisable(false);
                    readyBtn.setStyle(NORMAL_STYLE);
                    setStatus("Connected! Click Ready when you're set.", "#4CAF50");
                });

            } catch (IOException e) {
                Platform.runLater(
                        () -> setStatus("Could not connect to " + finalIp + " — is the server running?", "#F44336"));
            }
        }, "ConnectThread").start();
    }

    private void toggleReady() {
        if (!isConnected || client == null)
            return;
        isReady = !isReady;
        client.sendReady();
        readyBtn.setStyle(isReady ? READY_STYLE : NORMAL_STYLE);
        readyBtn.setText(isReady ? "Ready!" : "Ready");
        setStatus(isReady ? "Waiting for others…" : "Click Ready when you're set.", MUTED);
    }

    // ------------------------------------------------------------------
    // Server message handlers (JavaFX thread)
    // ------------------------------------------------------------------

    private void applyLobbyUpdate(List<LobbyPlayer> players) {
        playerList.getChildren().clear();

        int slots = Math.max(GameServer.MIN_PLAYERS, players.size());
        for (int i = 0; i < slots; i++) {
            if (i < players.size()) {
                playerList.getChildren().add(buildFilledSlot(i, players.get(i)));
            } else {
                playerList.getChildren().add(buildEmptySlot(i));
            }
        }

        long readyCount = players.stream().filter(p -> p.isReady).count();
        String prefix = "";
        if (currentServerIp != null && !currentServerIp.isEmpty()) {
            String code = encodeIp(currentServerIp);
            prefix = "Host IP: " + currentServerIp + "  |  Code: " + code + "  |  ";
        }
        setStatus(prefix + "Players: " + players.size() + "/" + GameServer.MIN_PLAYERS
                + "  |  Ready: " + readyCount + "/" + players.size(), MUTED);
    }

    private String encodeIp(String ip) {
        if (ip.equals("localhost") || ip.equals("127.0.0.1"))
            return "LOCAL";
        try {
            String[] parts = ip.split("\\.");
            long num = 0;
            for (int i = 0; i < 4; i++) {
                num = (num << 8) | Integer.parseInt(parts[i]);
            }
            return Long.toString(num, 36).toUpperCase();
        } catch (Exception e) {
            return "ERROR";
        }
    }

    private String decodeIp(String code) {
        if (code.equalsIgnoreCase("LOCAL"))
            return "127.0.0.1";
        try {
            long num = Long.parseLong(code, 36);
            return ((num >> 24) & 0xFF) + "." +
                    ((num >> 16) & 0xFF) + "." +
                    ((num >> 8) & 0xFF) + "." +
                    (num & 0xFF);
        } catch (Exception e) {
            return code;
        }
    }

    private void startGame(NetworkMessage msg) {
        mainApp.showMultiplayerGame(client, msg.spawnX, msg.spawnY, msg.colorHex, msg.playerId);
    }

    // ------------------------------------------------------------------
    // Slot builders
    // ------------------------------------------------------------------

    private StackPane buildFilledSlot(int index, LobbyPlayer player) {
        StackPane card = new StackPane();
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(165, 185);
        card.setStyle(
                "-fx-background-color: rgba(61,40,46,0.90); -fx-background-radius: 14;" +
                "-fx-border-color: " + GOLD + "; -fx-border-width: 3; -fx-border-radius: 14;");

        int imgIdx = Math.abs(player.colorHex.hashCode()) % DOUGH_FILES.length;
        ImageView icon = new ImageView();
        if (doughImages[imgIdx] != null && !doughImages[imgIdx].isError())
            icon.setImage(doughImages[imgIdx]);
        icon.setFitWidth(80);
        icon.setFitHeight(80);
        icon.setPreserveRatio(true);

        Label name = new Label(player.playerName + (player.isReady ? " ✓" : ""));
        name.setFont(FONT_SMALL);
        name.setStyle("-fx-text-fill: " + CREAM + "; -fx-font-weight: bold;");
        name.setWrapText(true);
        name.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        name.setAlignment(Pos.CENTER);
        name.setMaxWidth(150);

        VBox inner = new VBox(10, icon, name);
        inner.setAlignment(Pos.CENTER);
        card.getChildren().add(inner);
        return card;
    }

    private StackPane buildEmptySlot(int index) {
        StackPane card = new StackPane();
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(165, 185);
        card.setStyle(
                "-fx-background-color: rgba(20,15,18,0.50); -fx-background-radius: 14;" +
                "-fx-border-color: #4a3a2a; -fx-border-width: 2; -fx-border-radius: 14;" +
                "-fx-border-style: dashed;");

        int imgIdx = index % DOUGH_FILES.length;
        ImageView icon = new ImageView();
        if (doughImages[imgIdx] != null && !doughImages[imgIdx].isError())
            icon.setImage(doughImages[imgIdx]);
        icon.setFitWidth(80);
        icon.setFitHeight(80);
        icon.setPreserveRatio(true);
        icon.setOpacity(0.20);

        Label waiting = new Label("Waiting...");
        waiting.setFont(FONT_SMALL);
        waiting.setStyle("-fx-text-fill: #5a4a3a;");
        waiting.setAlignment(Pos.CENTER);

        VBox inner = new VBox(10, icon, waiting);
        inner.setAlignment(Pos.CENTER);
        card.getChildren().add(inner);
        return card;
    }

    private StackPane slotBase(int index) {
        StackPane slot = new StackPane();
        slot.setAlignment(Pos.CENTER_LEFT);
        slot.setPrefSize(300, 68);
        slot.setStyle(
                "-fx-background-color: rgba(61,40,46,0.75); -fx-background-radius: 8;" +
                        "-fx-border-color: " + GOLD + "; -fx-border-width: 2; -fx-border-radius: 8;");
        return slot;
    }

    private StackPane buildArenaPreview() {
        StackPane preview = new StackPane();
        preview.setPrefSize(300, 300);
        preview.setMaxSize(300, 300);

        javafx.scene.layout.Pane imagePane = new javafx.scene.layout.Pane();
        imagePane.setMinSize(300, 300);
        imagePane.setMaxSize(300, 300);

        if (arenaPreviewBgImage != null && !arenaPreviewBgImage.isError()) {
            ImageView bg = new ImageView(arenaPreviewBgImage);
            bg.setFitHeight(300);
            bg.setPreserveRatio(true);
            bg.setLayoutX(-116);
            imagePane.getChildren().add(bg);
        }

        Circle clip = new Circle(150, 150, 145);
        imagePane.setClip(clip);

        Circle border = new Circle(150, 150, 145);
        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.web(GOLD));
        border.setStrokeWidth(3);
        imagePane.getChildren().add(border);

        preview.getChildren().add(imagePane);

        Label previewLabel = new Label("Arena Preview");
        previewLabel.setFont(FONT_FIELD);
        previewLabel.setStyle("-fx-text-fill: " + CREAM + "; -fx-font-weight: bold;");
        StackPane.setAlignment(previewLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(previewLabel, new Insets(0, 0, 14, 0));
        preview.getChildren().add(previewLabel);

        return preview;
    }

    private VBox buildPreviewStrip() {
        Label header = new Label("What's in the kitchen:");
        header.setFont(FONT_FIELD);
        header.setStyle("-fx-text-fill: " + MUTED + ";");

        HBox iconRow = new HBox(18);
        iconRow.setAlignment(Pos.CENTER);
        for (Image img : previewImages) {
            if (img != null && !img.isError()) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(32);
                iv.setFitHeight(32);
                iv.setPreserveRatio(true);
                iconRow.getChildren().add(iv);
            }
        }

        VBox strip = new VBox(8, header, iconRow);
        strip.setAlignment(Pos.CENTER);
        return strip;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void setStatus(String text, String hexColor) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill: " + hexColor + ";");
    }

    private void cleanup() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
        if (gameServer != null) {
            gameServer.stop();
            gameServer = null;
        }
    }

    public javafx.scene.Parent getRoot() {
        return root;
    }
}
