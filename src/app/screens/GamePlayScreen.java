package app.screens;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import app.Main;
import app.game_hud.StatOverlay;
import app.game_logic.BiggerSizePowerup;
import app.game_logic.FreezeHazard;
import app.game_logic.PickupEntity;
import app.game_logic.ReverseControlsHazard;
import app.game_logic.SlowingHazard;
import app.game_logic.SpeedPowerup;
import app.game_logic.TerritoryManager;
import app.game_logic.Timer;
import app.game_logic.TrailManager;
import app.game_logic.TransparentTrailPowerup;
import app.network.GameClient;
import app.network.NetworkMessage.PlayerState;
import app.utils.UIUtils;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

public class GamePlayScreen {

    // Font constants — loaded once, applied at construction, never re-set
    private static final Font FONT_TIMER = Font.font(UIUtils.MAIN_FONT, 24);
    private static final Font FONT_CHAT = Font.font(UIUtils.MAIN_FONT, 14);
    private static final Font FONT_TINY = Font.font(UIUtils.MAIN_FONT, 13);

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private final Pane root;
    private final Pane world;
    private final Main mainApp;

    private final TrailManager trailManager = new TrailManager();
    private final TerritoryManager territoryManager = new TerritoryManager();
    private final List<TrailManager> enemyTrailManagers = new ArrayList<>();

    private final Canvas overlayCanvas;
    private final GraphicsContext overlayGc;

    private ImageView playerSprite;

    // ---- Multiplayer networking ----
    private GameClient gameClient;
    private int myPlayerId = -1;

    private final java.util.Map<Integer, Canvas> remoteOverlays = new java.util.LinkedHashMap<>();
    private final java.util.Map<Integer, ImageView> remoteSprites = new java.util.LinkedHashMap<>();
    private final java.util.Map<Integer, TrailManager> remoteTrails = new java.util.LinkedHashMap<>();
    private final java.util.Map<Integer, TerritoryManager> remoteTerritories = new java.util.LinkedHashMap<>();
    private final java.util.Map<Integer, Color> remoteColors = new java.util.LinkedHashMap<>();
    private static final int NET_SEND_INTERVAL = 3;
    private int netFrameCounter = 0;

    private double playerX = 0;
    private double playerY = 0;
    private final double SPEED = 4.5;

    private double speedMultiplier = 1.0;
    private long speedEffectEndNanos = 0;

    // Current movement direction
    Set<KeyCode> pressedKeys = new HashSet<>();
    private double dirX = 1;
    private double dirY = 0;
    private double lastDirX = 1;
    private double lastDirY = 0;

    // Keyboard keys smoothness
    private double targetDirX = 1;
    private double targetDirY = 0;
    private final double TURN_SMOOTHNESS = 0.15;

    private enum InputMode {
        KEYBOARD, MOUSE
    }

    private InputMode activeInputMode = InputMode.KEYBOARD;

    private final double WORLD_RADIUS = 1500;

    private final Color PLAYER_COLOR;

    private static final Map<String, Color> DOUGH_COLORS = Map.of(
            "orange", Color.web("#FF7043"),
            "red", Color.web("#E53935"),
            "blue", Color.web("#1E88E5"),
            "green", Color.web("#43A047"),
            "yellow", Color.web("#FDD835"),
            "pink", Color.web("#EC407A"),
            "purple", Color.web("#8E24AA"),
            "indigo", Color.web("#3949AB"));

    private Label timerLabel;

    private String myPlayerName = "You";
    private StatOverlay statOverlay;
    private final Map<Integer, Double> remoteTerritoryPercents = new java.util.LinkedHashMap<>();
    private final Map<Integer, String> remotePlayerNames = new java.util.LinkedHashMap<>();

    private VBox chatBox;
    private VBox chatMessageArea;
    private boolean chatExpanded = false;
    private javafx.scene.control.ScrollPane chatScroll;
    private javafx.scene.control.TextField chatInput;

    private int ownedHexCount = 0;
    private final int totalHexCount = 1000;

    // tack active trail
    private boolean outsideTerritory = false;

    // ---- Performance: cached values to avoid per-frame recomputation ----
    /** Cached territory area fraction — only recomputed when territory changes. */
    private double cachedAreaFraction = 0.0;
    /**
     * Set to true when captureTerritory runs; cleared after fraction is
     * recalculated.
     */
    private boolean territoryDirty = true;
    /** Throttle leaderboard sort/rebuild — only update every N frames. */
    private int leaderboardThrottleCounter = 0;
    private static final int LEADERBOARD_UPDATE_INTERVAL = 10;
    /** Cached screen dimensions — avoid getWidth()/getHeight() overhead. */
    private double cachedScreenW = 0;
    private double cachedScreenH = 0;

    private boolean isDead = false;

    private final List<PickupEntity> pickups = new ArrayList<>();
    private Image bgImage;
    private Image playerSpriteImage;
    /** Dough name chosen at game start — passed to GameOverModal for the sprite. */
    private String chosenDough;

    private Image rollingPinSprite;
    private long lastHazardSpawnNanos = 0;
    /** How often to spawn a new Rolling Pin hazard (8 seconds). */
    private static final long HAZARD_SPAWN_INTERVAL_NANOS = 8_000_000_000L;

    /** True while the player is frozen by H2 Ice Spill. */
    private boolean isFrozen = false;
    /** Nanosecond timestamp when the freeze effect expires. */
    private long freezeEndNanos = 0;
    /** Sprite for H2 Ice Spill hazard; null if the file is missing. */
    private Image iceSprite;
    /** Nanosecond timestamp of the last Ice Spill spawn (0 = none yet). */
    private long lastIceSpawnNanos = 0;
    /** How often to spawn a new Ice Spill hazard (12 seconds). */
    private static final long ICE_SPAWN_INTERVAL_NANOS = 12_000_000_000L;

    /** True while the player's controls are reversed by H3 Rotten Egg. */
    private boolean isControlsReversed = false;
    /** Nanosecond timestamp when the reverse-controls effect expires. */
    private long reverseEndNanos = 0;
    /** Sprite for H3 Rotten Egg hazard; null if the file is missing. */
    private Image rottenEggSprite;
    private long lastRottenEggSpawnNanos = 0;
    /** How often to spawn a new Rotten Egg hazard (15 seconds). */
    private static final long ROTTEN_EGG_SPAWN_INTERVAL_NANOS = 15_000_000_000L;

    // --- Powerups ---
    private boolean isTrailTransparent = false;
    private long transparentEndNanos = 0;
    private long biggerSizeEndNanos = 0;

    private Image oilSprite;
    private long lastOilSpawnNanos = 0;
    private static final long OIL_SPAWN_INTERVAL_NANOS = 15_000_000_000L;

    private Image doughPowerupSprite;
    private long lastDoughSpawnNanos = 0;
    private static final long DOUGH_SPAWN_INTERVAL_NANOS = 20_000_000_000L;

    private Image flourSprite;
    private long lastFlourSpawnNanos = 0;
    private static final long FLOUR_SPAWN_INTERVAL_NANOS = 25_000_000_000L;

    /** Shared RNG — used for dough selection and hazard spawning. */
    private final Random rng = new Random();

    /** Named game loop so it can be stopped on game-over. */
    private AnimationTimer gameLoop;

    /** Countdown timer from develop branch. */
    private Timer gameTimer;

    // -----------------------------------------------------------------------
    // Constructor / Setup
    // -----------------------------------------------------------------------

    public GamePlayScreen(Main mainApp, GameClient client, double spawnX, double spawnY, String colorHex,
            int myPlayerId) {
        this(mainApp, getDoughFromHex(colorHex)); // Runs the full setup with the correct dough color

        // Override defaults set by the single-player constructor
        this.gameClient = client;
        this.myPlayerId = myPlayerId;
        this.myPlayerName = client.getPlayerName();

        // Teleport to server-assigned spawn
        this.playerX = spawnX;
        this.playerY = spawnY;
        if (playerSprite != null) {
            playerSprite.setX(playerX - 50);
            playerSprite.setY(playerY - 50);
        }

        // Re-init territory at the correct spawn (single-player init used 0,0)
        territoryManager.clearTerritory();
        territoryManager.initStartingTerritory(playerX, playerY, 70);

        // Register the GAME_STATE callback — runs on network thread, touch UI on FX
        // thread
        client.onGameState(states -> Platform.runLater(() -> applyRemoteStates(states)));
        client.onPlayerDied(deadId -> Platform.runLater(() -> removeRemotePlayer(deadId)));
        client.onChat(msg -> Platform.runLater(() -> appendChatMessage(msg.playerName, msg.message, msg.colorHex)));
        client.onGameOver(results -> Platform.runLater(() -> {
            if (!isDead) {
                isDead = true;
                showMultiplayerGameOver(results);
            }
        }));
    }

    // Helper to map network hex colors back to sprite names
    private static String getDoughFromHex(String hex) {
        if (hex == null)
            return "orange";
        for (Map.Entry<String, Color> entry : DOUGH_COLORS.entrySet()) {
            String colorHex = "#" + entry.getValue().toString().substring(2, 8).toUpperCase();
            if (colorHex.equalsIgnoreCase(hex)) {
                return entry.getKey();
            }
        }
        return "orange";
    }

    // Single Player
    public GamePlayScreen(Main mainApp, String doughOverride) {
        this.mainApp = mainApp;

        root = new Pane();

        // --- Background ---
        File bgFile = new File("assets/images/GameplayBackground.jpg");
        if (bgFile.exists()) {
            bgImage = UIUtils.ImageCache.get("assets/images/GameplayBackground.jpg");
            javafx.scene.layout.BackgroundImage background = new javafx.scene.layout.BackgroundImage(
                    bgImage,
                    javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                    javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                    javafx.scene.layout.BackgroundPosition.CENTER,
                    new javafx.scene.layout.BackgroundSize(
                            javafx.scene.layout.BackgroundSize.AUTO,
                            javafx.scene.layout.BackgroundSize.AUTO,
                            false, false, false, true));
            root.setBackground(new javafx.scene.layout.Background(background));
        } else {
            root.setStyle("-fx-background-color: #111111;");
        }

        // --- World pane ---
        world = new Pane();
        root.getChildren().add(world);

        // --- Arena circle ---
        Circle arena = new Circle(0, 0, WORLD_RADIUS);
        arena.setFill(Color.web("#f8f1df", 0.8));
        arena.setStroke(Color.WHITE);
        arena.setStrokeWidth(10);
        world.getChildren().add(arena);

        // --- Hex grid overlay (static — drawn once onto a canvas) ---
        Canvas hexCanvas = new Canvas(WORLD_RADIUS * 2, WORLD_RADIUS * 2);
        hexCanvas.setTranslateX(-WORLD_RADIUS);
        hexCanvas.setTranslateY(-WORLD_RADIUS);
        drawHexGrid(hexCanvas.getGraphicsContext2D());
        world.getChildren().add(hexCanvas);

        // --- Dynamic overlay: territory + trail redrawn every frame ---
        overlayCanvas = new Canvas(WORLD_RADIUS * 2, WORLD_RADIUS * 2);
        overlayCanvas.setTranslateX(-WORLD_RADIUS);
        overlayCanvas.setTranslateY(-WORLD_RADIUS);
        overlayGc = overlayCanvas.getGraphicsContext2D();
        world.getChildren().add(overlayCanvas);

        // --- Player sprite ---
        if (doughOverride != null) {
            chosenDough = doughOverride;
        } else {
            String[] doughNames = { "orange", "red", "blue", "green", "yellow", "pink", "purple", "indigo" };
            chosenDough = doughNames[rng.nextInt(doughNames.length)];
        }
        PLAYER_COLOR = DOUGH_COLORS.getOrDefault(chosenDough, Color.web("#FF7043"));

        File playerFile = new File("assets/images/PlayersDough/" + chosenDough + ".png");
        if (playerFile.exists()) {
            playerSpriteImage = UIUtils.ImageCache.get("assets/images/PlayersDough/" + chosenDough + ".png");
            playerSprite = new ImageView(playerSpriteImage);
            playerSprite.setPreserveRatio(true);
            playerSprite.setSmooth(true);
            playerSprite.setFitWidth(100);
            playerSprite.setFitHeight(100);
        }
        if (playerSprite != null)
            world.getChildren().add(playerSprite);

        // --- H1 Rolling Pin hazard sprite ---
        File rpFile = new File("assets/images/hazard/RollingPin-Hazard.png");
        if (rpFile.exists()) {
            rollingPinSprite = UIUtils.ImageCache.get("assets/images/hazard/RollingPin-Hazard.png");
        }

        // --- H2 Ice Spill hazard sprite ---
        File iceFile = new File("assets/images/hazard/Ice-Hazard.png");
        if (iceFile.exists()) {
            iceSprite = UIUtils.ImageCache.get("assets/images/hazard/Ice-Hazard.png");
        }

        File reFile = new File("assets/images/hazard/RottenEgg-Hazard.png");
        if (reFile.exists()) {
            rottenEggSprite = UIUtils.ImageCache.get("assets/images/hazard/RottenEgg-Hazard.png");
        }

        // --- Powerup sprites ---
        File oilFile = new File("assets/images/powerup/Oil-Powerup.png");
        if (oilFile.exists()) {
            oilSprite = UIUtils.ImageCache.get("assets/images/powerup/Oil-Powerup.png");
        }
        File doughPFile = new File("assets/images/powerup/Dough-Powerup.png");
        if (doughPFile.exists()) {
            doughPowerupSprite = UIUtils.ImageCache.get("assets/images/powerup/Dough-Powerup.png");
        }
        File flourFile = new File("assets/images/powerup/Flour-Powerup.png");
        if (flourFile.exists()) {
            flourSprite = UIUtils.ImageCache.get("assets/images/powerup/Flour-Powerup.png");
        }

        // --- Starting territory centred on spawn ---
        territoryManager.initStartingTerritory(playerX, playerY, 70);

        // --- HUD ---

        // --- Leaderboard overlay ---
        statOverlay = new StatOverlay();
        root.getChildren().add(statOverlay.getRoot());

        // --- Timer HUD (from develop branch) ---
        timerLabel = new Label("Time: 00:00");
        timerLabel.setFont(FONT_TIMER);
        timerLabel.setStyle("-fx-text-fill: white; -fx-effect: dropshadow(gaussian,black,4,0.6,0,0);");
        root.getChildren().add(timerLabel);

        gameTimer = new Timer(
                40, // seconds (set to desired game duration)
                () -> javafx.application.Platform.runLater(
                        () -> timerLabel.setText("Time: " + gameTimer.getFormattedTime())),
                () -> {
                    System.out.println("Timer reached zero!");
                    // Issue 2: In multiplayer the server owns the timer and will
                    // broadcast GAME_OVER — the client-side timer must NOT also
                    // trigger showGameOver() or the two will conflict.
                    if (gameClient != null)
                        return;
                    if (!isDead) {
                        isDead = true;
                        showGameOver();
                    }
                });
        gameTimer.start();

        // --- Chat box (bottom-right, collapsible) ---
        chatMessageArea = new VBox(4);
        chatMessageArea.setPadding(new Insets(6, 8, 6, 8));

        Label chatPlaceholder = new Label("No messages yet");
        chatPlaceholder.setFont(FONT_TINY);
        chatPlaceholder.setStyle("-fx-text-fill: #666666;");
        chatMessageArea.getChildren().add(chatPlaceholder);

        chatScroll = new ScrollPane(chatMessageArea);
        chatScroll.setPrefWidth(256);
        chatScroll.setPrefHeight(180);
        chatScroll.setFitToWidth(true);
        chatScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chatScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        chatScroll.setVisible(false);
        chatScroll.setManaged(false);

        Button chatToggle = new Button("Chat ▲");
        chatToggle.setFont(FONT_CHAT);
        chatToggle.setStyle(
                "-fx-background-color: rgba(0,0,0,0.65);" +
                        "-fx-text-fill: #f0d090;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 4 12;" +
                        "-fx-cursor: hand;");
        chatToggle.setMaxWidth(Double.MAX_VALUE);
        chatInput = new javafx.scene.control.TextField();
        chatInput.setPromptText("Type a message...");
        chatInput.setFont(FONT_CHAT);
        chatInput.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-text-fill: #333333; -fx-padding: 4 8; -fx-background-radius: 4;");
        
        Button chatSendBtn = new Button("Send");
        chatSendBtn.setFont(FONT_CHAT);
        chatSendBtn.setStyle("-fx-background-color: #f0d090; -fx-text-fill: #333333; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
        
        Runnable sendChatAction = () -> {
            String text = chatInput.getText().trim();
            if (!text.isEmpty()) {
                if (text.length() > 100) text = text.substring(0, 100);
                if (gameClient != null) {
                    final String msgToSend = text;
                    new Thread(() -> gameClient.sendChat(msgToSend)).start();
                } else {
                    // Local echo for single player testing
                    appendChatMessage(myPlayerName, text, PLAYER_COLOR.toString().replace("0x", "#"));
                }
                chatInput.clear();
            }
            root.requestFocus();
        };
        
        chatSendBtn.setOnAction(e -> sendChatAction.run());
        chatInput.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                sendChatAction.run();
                e.consume();
            }
        });

        javafx.scene.layout.HBox chatInputBox = new javafx.scene.layout.HBox(4, chatInput, chatSendBtn);
        chatInputBox.setPadding(new Insets(4));
        chatInputBox.setVisible(false);
        chatInputBox.setManaged(false);
        javafx.scene.layout.HBox.setHgrow(chatInput, javafx.scene.layout.Priority.ALWAYS);

        chatToggle.setOnAction(e -> {
            chatExpanded = !chatExpanded;
            chatScroll.setVisible(chatExpanded);
            chatScroll.setManaged(chatExpanded);
            chatInputBox.setVisible(chatExpanded);
            chatInputBox.setManaged(chatExpanded);
            chatToggle.setText(chatExpanded ? "Chat ▼" : "Chat ▲");
            if (chatExpanded) {
                chatInput.requestFocus();
            } else {
                root.requestFocus(); // return focus to game after button click
            }
        });

        chatBox = new VBox(0, chatScroll, chatInputBox, chatToggle);
        chatBox.setStyle(
                "-fx-background-color: rgba(0,0,0,0.55);" +
                        "-fx-background-radius: 10;");
        chatBox.setPrefWidth(260);
        root.getChildren().add(chatBox);

        // --- Input ---
        root.setFocusTraversable(true);

        root.setOnMouseMoved(e -> {
            double dx = e.getX() - (root.getWidth() / 2);
            double dy = e.getY() - (root.getHeight() / 2);
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len > 1) {
                activeInputMode = InputMode.MOUSE;
                targetDirX = dx / len;
                targetDirY = dy / len;
                lastDirX = targetDirX;
                lastDirY = targetDirY;
            }
        });

        // key handling
        root.setOnKeyPressed(e -> {
            if (chatInput != null && chatInput.isFocused()) return;

            activeInputMode = InputMode.KEYBOARD;
            pressedKeys.add(e.getCode());
            updateDirection();

            if (e.getCode() == KeyCode.ESCAPE) {
                mainApp.showLandingPage();
            }
        });

        root.setOnKeyReleased(e -> {
            if (chatInput != null && chatInput.isFocused()) return;

            pressedKeys.remove(e.getCode());
            updateDirection();
        });

        // --- Game loop (named so showGameOver can stop it) ---
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update(now);
            }
        };
        gameLoop.start();
    }

    // -----------------------------------------------------------------------
    // Game loop
    // -----------------------------------------------------------------------

    private void update(long now) {
        double screenW = root.getWidth();
        double screenH = root.getHeight();
        if (screenW == 0)
            return;
        cachedScreenW = screenW;
        cachedScreenH = screenH;

        // --- Expire timed effects ---
        if (speedMultiplier != 1.0 && now >= speedEffectEndNanos) {
            speedMultiplier = 1.0;
        }
        if (isFrozen && now >= freezeEndNanos) {
            isFrozen = false;
        }
        if (isControlsReversed && now >= reverseEndNanos) {
            isControlsReversed = false;
        }
        if (now >= biggerSizeEndNanos) {
            trailManager.setWidthMultiplier(1.0);
        }
        if (isTrailTransparent && now >= transparentEndNanos) {
            isTrailTransparent = false;
        }

        // Smooth Direction — inversion is applied here as a read-only local so
        // neither the mouse handler nor updateDirection() need to know about H3.
        double effectiveDirX = isControlsReversed ? -targetDirX : targetDirX;
        double effectiveDirY = isControlsReversed ? -targetDirY : targetDirY;
        dirX += (effectiveDirX - dirX) * TURN_SMOOTHNESS;
        dirY += (effectiveDirY - dirY) * TURN_SMOOTHNESS;

        // re-normalize to keep constant speed
        double len = Math.sqrt(dirX * dirX + dirY * dirY);
        if (len > 0) {
            dirX /= len;
            dirY /= len;
        }

        // --- Move player (skipped while frozen; collision checks below still run) ---
        if (!isFrozen) {
            playerX += dirX * SPEED * speedMultiplier;
            playerY += dirY * SPEED * speedMultiplier;
        }

        // Clamp inside arena
        double dist = Math.sqrt(playerX * playerX + playerY * playerY);
        double maxR = WORLD_RADIUS - 30;
        if (dist > maxR) {
            double angle = Math.atan2(playerY, playerX);
            playerX = Math.cos(angle) * maxR;
            playerY = Math.sin(angle) * maxR;
        }

        // --- Hazard spawn ---
        if (lastHazardSpawnNanos == 0 || now - lastHazardSpawnNanos >= HAZARD_SPAWN_INTERVAL_NANOS) {
            spawnRollingPinHazard();
            lastHazardSpawnNanos = now;
        }
        if (lastIceSpawnNanos == 0 || now - lastIceSpawnNanos >= ICE_SPAWN_INTERVAL_NANOS) {
            spawnIceHazard();
            lastIceSpawnNanos = now;
        }
        if (lastRottenEggSpawnNanos == 0 || now - lastRottenEggSpawnNanos >= ROTTEN_EGG_SPAWN_INTERVAL_NANOS) {
            spawnRottenEggHazard();
            lastRottenEggSpawnNanos = now;
        }
        if (lastOilSpawnNanos == 0 || now - lastOilSpawnNanos >= OIL_SPAWN_INTERVAL_NANOS) {
            spawnPickup(oilSprite, (hx, hy, spr) -> new SpeedPowerup(hx, hy, spr));
            lastOilSpawnNanos = now;
        }
        if (lastDoughSpawnNanos == 0 || now - lastDoughSpawnNanos >= DOUGH_SPAWN_INTERVAL_NANOS) {
            spawnPickup(doughPowerupSprite, (hx, hy, spr) -> new BiggerSizePowerup(hx, hy, spr));
            lastDoughSpawnNanos = now;
        }
        if (lastFlourSpawnNanos == 0 || now - lastFlourSpawnNanos >= FLOUR_SPAWN_INTERVAL_NANOS) {
            spawnPickup(flourSprite, (hx, hy, spr) -> new TransparentTrailPowerup(hx, hy, spr));
            lastFlourSpawnNanos = now;
        }

        // --- Pickup collision ---
        pickups.removeIf(p -> !p.isActive());
        for (PickupEntity pickup : pickups) {
            if (pickup.isContactedBy(playerX, playerY)) {
                pickup.despawn();
                if (pickup instanceof SlowingHazard sh) {
                    speedMultiplier = SlowingHazard.SPEED_MULTIPLIER;
                    speedEffectEndNanos = now + (long) (sh.getEffectDurationSeconds() * 1_000_000_000L);
                } else if (pickup instanceof FreezeHazard fh) {
                    isFrozen = true;
                    freezeEndNanos = now + (long) (fh.getEffectDurationSeconds() * 1_000_000_000L);
                } else if (pickup instanceof ReverseControlsHazard rh) {
                    isControlsReversed = true;
                    reverseEndNanos = now + (long) (rh.getEffectDurationSeconds() * 1_000_000_000L);
                } else if (pickup instanceof SpeedPowerup sp) {
                    speedMultiplier = SpeedPowerup.SPEED_MULTIPLIER;
                    speedEffectEndNanos = now + (long) (sp.getEffectDurationSeconds() * 1_000_000_000L);
                } else if (pickup instanceof BiggerSizePowerup bp) {
                    trailManager.setWidthMultiplier(BiggerSizePowerup.TRAIL_SIZE_MULTIPLIER);
                    biggerSizeEndNanos = now + (long) (bp.getEffectDurationSeconds() * 1_000_000_000L);
                } else if (pickup instanceof TransparentTrailPowerup tp) {
                    isTrailTransparent = true;
                    transparentEndNanos = now + (long) (tp.getEffectDurationSeconds() * 1_000_000_000L);
                }
            }
        }

        // --- Territory check ---
        boolean insideTerr = territoryManager.isInsideTerritory(playerX, playerY);

        // Track whether the player is currently outside (trail recording)
        if (!insideTerr)
            outsideTerritory = true;

        // --- Update trail ---
        boolean captured = trailManager.update(playerX, playerY, insideTerr);

        // --- Capture event ---
        if (captured) {
            outsideTerritory = false;
            List<Point2D> trail = trailManager.getTrailPoints();
            territoryManager.captureTerritory(trail);
            trailManager.clear();
            territoryDirty = true; // trigger area fraction recalculation
        }

        // --- Self-collision check (only while trail is active) ---
        if (trailManager.isActive() && trailManager.checkSelfCollision(playerX, playerY)) {
            handleDeath();
            return;
        }

        // --- Enemy trail collision check ---
        for (TrailManager enemyTrail : enemyTrailManagers) {
            if (enemyTrail.checkEnemyCollision(playerX, playerY)) {
                handleDeath();
                return;
            }
        }

        // --- Update sprite position ---
        if (playerSprite != null) {
            playerSprite.setX(playerX - 50);
            playerSprite.setY(playerY - 50);
        }

        // --- Render overlay (territory + trail) — viewport-clipped clear ---
        // Only clear the visible portion of the 3000x3000 canvas (huge perf win).
        double vpX = WORLD_RADIUS + playerX - screenW / 2 - 2;
        double vpY = WORLD_RADIUS + playerY - screenH / 2 - 2;
        double vpW = screenW + 4;
        double vpH = screenH + 4;
        overlayGc.clearRect(vpX, vpY, vpW, vpH);
        overlayGc.save();
        overlayGc.beginPath();
        overlayGc.arc(WORLD_RADIUS, WORLD_RADIUS, WORLD_RADIUS - 5, WORLD_RADIUS - 5, 0, 360);
        overlayGc.clip();
        overlayGc.translate(WORLD_RADIUS, WORLD_RADIUS);
        territoryManager.drawTerritory(overlayGc, PLAYER_COLOR);
        trailManager.draw(overlayGc, PLAYER_COLOR);
        for (PickupEntity pickup : pickups) {
            pickup.draw(overlayGc);
        }
        overlayGc.restore();

        // --- Follow camera ---
        world.setTranslateX((screenW / 2) - playerX);
        world.setTranslateY((screenH / 2) - playerY);

        // --- HUD: use cached area fraction (only recomputed after capture) ---
        if (territoryDirty) {
            cachedAreaFraction = territoryManager.getApproximateAreaFraction(Math.PI * WORLD_RADIUS * WORLD_RADIUS);
            ownedHexCount = (int) (cachedAreaFraction * totalHexCount);
            territoryDirty = false;
        }

        // --- Leaderboard update: throttled to every LEADERBOARD_UPDATE_INTERVAL frames
        // ---
        leaderboardThrottleCounter++;
        if (leaderboardThrottleCounter >= LEADERBOARD_UPDATE_INTERVAL) {
            leaderboardThrottleCounter = 0;
            List<StatOverlay.PlayerEntry> leaderboard = new ArrayList<>();
            leaderboard.add(new StatOverlay.PlayerEntry(myPlayerName, PLAYER_COLOR, cachedAreaFraction * 100));
            for (Map.Entry<Integer, Color> entry : remoteColors.entrySet()) {
                double pct = remoteTerritoryPercents.getOrDefault(entry.getKey(), 0.0);
                String name = remotePlayerNames.getOrDefault(entry.getKey(), "Player " + entry.getKey());
                leaderboard.add(new StatOverlay.PlayerEntry(name, entry.getValue(), pct));
            }
            leaderboard.sort((a, b) -> Double.compare(b.territoryPercent, a.territoryPercent));
            statOverlay.update(leaderboard);
        }

        timerLabel.setLayoutX((screenW - timerLabel.getWidth()) / 2);
        timerLabel.setLayoutY(14);

        chatBox.setLayoutX(screenW - chatBox.getPrefWidth() - 14);
        chatBox.setLayoutY(screenH - chatBox.getHeight() - 14);

        // --- Multiplayer: send position to server every NET_SEND_INTERVAL frames ---
        if (gameClient != null && gameClient.isConnected()) {
            netFrameCounter++;
            if (netFrameCounter >= NET_SEND_INTERVAL) {
                netFrameCounter = 0;
                double[] packed;
                if (isTrailTransparent) {
                    packed = new double[0];
                } else {
                    List<Point2D> trailPts = trailManager.getTrailPoints();
                    packed = new double[trailPts.size() * 2];
                    for (int i = 0; i < trailPts.size(); i++) {
                        packed[i * 2] = trailPts.get(i).getX();
                        packed[i * 2 + 1] = trailPts.get(i).getY();
                    }
                }
                gameClient.sendPositionUpdate(playerX, playerY, dirX, dirY, packed, cachedAreaFraction * 100);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Hazard and Powerup spawning
    // -----------------------------------------------------------------------

    private interface PickupFactory {
        PickupEntity create(double x, double y, Image sprite);
    }

    private void spawnPickup(Image sprite, PickupFactory factory) {
        if (sprite == null)
            return;
        double maxR = WORLD_RADIUS * 0.85;
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = rng.nextDouble() * 2 * Math.PI;
            double r = rng.nextDouble() * maxR;
            double hx = Math.cos(angle) * r;
            double hy = Math.sin(angle) * r;
            if (!territoryManager.isInsideTerritory(hx, hy)) {
                pickups.add(factory.create(hx, hy, sprite));
                return;
            }
        }
    }

    /**
     * Spawns a Rolling Pin (H1) hazard at a random position inside the arena
     * that is not covered by the player's territory.
     * Up to 20 attempts; silently skips if no valid tile is found.
     */
    private void spawnRollingPinHazard() {
        if (rollingPinSprite == null)
            return;
        double maxR = WORLD_RADIUS * 0.85;
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = rng.nextDouble() * 2 * Math.PI;
            double r = rng.nextDouble() * maxR;
            double hx = Math.cos(angle) * r;
            double hy = Math.sin(angle) * r;
            if (!territoryManager.isInsideTerritory(hx, hy)) {
                pickups.add(new SlowingHazard(hx, hy, rollingPinSprite));
                return;
            }
        }
    }

    /**
     * Spawns a Rotten Egg (H3) hazard at a random arena position outside territory.
     */
    private void spawnRottenEggHazard() {
        if (rottenEggSprite == null)
            return;
        double maxR = WORLD_RADIUS * 0.85;
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = rng.nextDouble() * 2 * Math.PI;
            double r = rng.nextDouble() * maxR;
            double hx = Math.cos(angle) * r;
            double hy = Math.sin(angle) * r;
            if (!territoryManager.isInsideTerritory(hx, hy)) {
                pickups.add(new ReverseControlsHazard(hx, hy, rottenEggSprite));
                return;
            }
        }
    }

    /**
     * Spawns an Ice Spill (H2) hazard at a random arena position outside territory.
     */
    private void spawnIceHazard() {
        if (iceSprite == null)
            return;
        double maxR = WORLD_RADIUS * 0.85;
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = rng.nextDouble() * 2 * Math.PI;
            double r = rng.nextDouble() * maxR;
            double hx = Math.cos(angle) * r;
            double hy = Math.sin(angle) * r;
            if (!territoryManager.isInsideTerritory(hx, hy)) {
                pickups.add(new FreezeHazard(hx, hy, iceSprite));
                return;
            }
        }
    }

    // -----------------------------------------------------------------------
    // Death
    // -----------------------------------------------------------------------

    private void handleDeath() {
        if (isDead)
            return;
        isDead = true;
        trailManager.clear();
        territoryManager.clearTerritory();
        pickups.clear();
        speedMultiplier = 1.0;
        isFrozen = false;
        isControlsReversed = false;
        outsideTerritory = false;
        trailManager.setWidthMultiplier(1.0);
        biggerSizeEndNanos = 0;
        isTrailTransparent = false;
        transparentEndNanos = 0;

        if (gameClient != null) {
            // Issue 1: Multiplayer — show game-over immediately for this (dead) player
            // while others may still be playing. Build a snapshot of known results.
            java.util.List<app.network.NetworkMessage.GameResult> snapshot = new java.util.ArrayList<>();

            // Local player just died — territory is 0
            String myHex = "#" + PLAYER_COLOR.toString().substring(2, 8).toUpperCase();
            snapshot.add(new app.network.NetworkMessage.GameResult(
                    myPlayerId, myPlayerName, myHex, 0.0, 0));

            for (java.util.Map.Entry<Integer, Color> entry : remoteColors.entrySet()) {
                int id = entry.getKey();
                Color col = entry.getValue();
                double pct = remoteTerritoryPercents.getOrDefault(id, 0.0);
                String name = remotePlayerNames.getOrDefault(id, "Player " + id);
                String hex = "#" + col.toString().substring(2, 8).toUpperCase();
                snapshot.add(new app.network.NetworkMessage.GameResult(id, name, hex, pct, 0));
            }

            // Sort descending by territory %, assign ranks
            snapshot.sort((a, b) -> Double.compare(b.territoryPercent, a.territoryPercent));
            for (int i = 0; i < snapshot.size(); i++)
                snapshot.get(i).rank = i + 1;

            // Disconnect so the server detects this player left and triggers
            // checkLastPlayerStanding() for the remaining players.
            gameClient.disconnect();

            showMultiplayerGameOver(snapshot);
        } else {
            showGameOver();
        }
    }

    // -----------------------------------------------------------------------
    // Game Over
    // -----------------------------------------------------------------------

    private void showGameOver() {
        gameTimer.stop();
        gameLoop.stop();

        // Use cachedAreaFraction — TerritoryManager is already cleared by handleDeath()
        // so querying it directly would always return 0.0%.
        double territoryPct = cachedAreaFraction * 100.0;
        String spritePath = "assets/images/PlayersDough/" + chosenDough + ".png";
        GameOverModal modal = new GameOverModal(mainApp, territoryPct, spritePath);
        modal.show();
    }

    // -----------------------------------------------------------------------
    // Update (key press)
    // -----------------------------------------------------------------------
    private void updateDirection() {
        targetDirX = 0;
        targetDirY = 0;

        if (pressedKeys.contains(KeyCode.W) || pressedKeys.contains(KeyCode.UP)) {
            targetDirY -= 1;
        }
        if (pressedKeys.contains(KeyCode.S) || pressedKeys.contains(KeyCode.DOWN)) {
            targetDirY += 1;
        }
        if (pressedKeys.contains(KeyCode.A) || pressedKeys.contains(KeyCode.LEFT)) {
            targetDirX -= 1;
        }
        if (pressedKeys.contains(KeyCode.D) || pressedKeys.contains(KeyCode.RIGHT)) {
            targetDirX += 1;
        }

        // normalize target direction
        if (targetDirX != 0 || targetDirY != 0) {
            double len = Math.sqrt(targetDirX * targetDirX + targetDirY * targetDirY);
            targetDirX /= len;
            targetDirY /= len;
            lastDirX = targetDirX;
            lastDirY = targetDirY;
        } else {
            targetDirX = lastDirX;
            targetDirY = lastDirY;
        }
    }

    // -----------------------------------------------------------------------
    // Static hex-grid drawing
    // -----------------------------------------------------------------------

    private void drawHexGrid(GraphicsContext gc) {
        gc.save();
        gc.beginPath();
        gc.arc(WORLD_RADIUS, WORLD_RADIUS, WORLD_RADIUS - 5, WORLD_RADIUS - 5, 0, 360);
        gc.clip();

        gc.setStroke(Color.web("#e0d5ba"));
        gc.setLineWidth(1.0);

        double hexRadius = 27.5;
        double hexW = Math.sqrt(3) * hexRadius;
        double hexH = 2 * hexRadius;

        for (double y = 0; y < WORLD_RADIUS * 2 + hexH; y += hexH * 0.75) {
            int row = (int) (y / (hexH * 0.75));
            double xOff = (row % 2 == 0) ? 0 : hexW / 2;

            for (double x = 0; x < WORLD_RADIUS * 2 + hexW; x += hexW) {
                double cx = x + xOff;
                double cy = y;
                double dx = cx - WORLD_RADIUS;
                double dy = cy - WORLD_RADIUS;
                if (Math.sqrt(dx * dx + dy * dy) <= WORLD_RADIUS + hexRadius) {
                    double[] xp = new double[6];
                    double[] yp = new double[6];
                    for (int i = 0; i < 6; i++) {
                        double a = Math.PI / 180 * (60 * i - 30);
                        xp[i] = cx + hexRadius * Math.cos(a);
                        yp[i] = cy + hexRadius * Math.sin(a);
                    }
                    gc.strokePolygon(xp, yp, 6);
                }
            }
        }
        gc.restore();
    }

    // -----------------------------------------------------------------------
    // Multiplayer — remote player rendering
    // -----------------------------------------------------------------------

    /**
     * Called on the JavaFX thread every time the server sends a GAME_STATE.
     * Updates or creates sprites/overlays for every remote player.
     */
    private void applyRemoteStates(java.util.List<PlayerState> states) {
        for (PlayerState state : states) {
            if (state.playerId == myPlayerId)
                continue; // skip self
            if (state.isDead) {
                removeRemotePlayer(state.playerId);
                continue;
            }

            Color color = remoteColors.computeIfAbsent(state.playerId,
                    id -> Color.web(state.colorHex));

            remoteTerritoryPercents.put(state.playerId, state.territoryPercent);
            if (state.playerName != null && !state.playerName.isEmpty()) {
                remotePlayerNames.put(state.playerId, state.playerName);
            }

            // ── Sprite ──────────────────────────────────────────────────
            ImageView sprite = remoteSprites.get(state.playerId);
            if (sprite == null) {
                sprite = createRemoteSprite(state.colorHex);
                remoteSprites.put(state.playerId, sprite);
                world.getChildren().add(sprite);
            }
            sprite.setX(state.x - 50);
            sprite.setY(state.y - 50);

            // ── Trail ───────────────────────────────────────────────────
            TrailManager trail = remoteTrails.computeIfAbsent(
                    state.playerId, id -> new TrailManager());

            // Sync trail points from packed array
            trail.clear();
            if (state.trailPoints != null && state.trailPoints.length >= 4) {
                // Inject points by calling update() repeatedly is complex; instead
                // we draw from the raw packed array directly below.
            }

            // ── Territory overlay ───────────────────────────────────────
            Canvas overlay = remoteOverlays.get(state.playerId);
            if (overlay == null) {
                overlay = new Canvas(WORLD_RADIUS * 2, WORLD_RADIUS * 2);
                overlay.setTranslateX(-WORLD_RADIUS);
                overlay.setTranslateY(-WORLD_RADIUS);
                // Insert behind the local player's overlay
                int localOverlayIdx = world.getChildren().indexOf(overlayCanvas);
                world.getChildren().add(Math.max(0, localOverlayIdx), overlay);
                remoteOverlays.put(state.playerId, overlay);
            }

            // Draw remote trail from packed points
            // Viewport-clipped clear: only wipe the pixels currently on screen.
            GraphicsContext gc = overlay.getGraphicsContext2D();
            double rvpX = WORLD_RADIUS + playerX - cachedScreenW / 2 - 2;
            double rvpY = WORLD_RADIUS + playerY - cachedScreenH / 2 - 2;
            double rvpW = cachedScreenW + 4;
            double rvpH = cachedScreenH + 4;
            gc.clearRect(rvpX, rvpY, rvpW, rvpH);
            gc.save();
            gc.beginPath();
            gc.arc(WORLD_RADIUS, WORLD_RADIUS, WORLD_RADIUS - 5, WORLD_RADIUS - 5, 0, 360);
            gc.clip();
            gc.translate(WORLD_RADIUS, WORLD_RADIUS);
            drawRemoteTrail(gc, state.trailPoints, color);
            gc.restore();

            // ── Enemy trail collision (self → die if touching their trail) ──
            if (trailManager.isActive() && state.trailPoints != null && state.trailPoints.length >= 4) {
                if (checkEnemyTrailCollision(playerX, playerY, state.trailPoints)) {
                    handleDeath();
                    return;
                }
            }
        }
    }

    /** Draws a remote player's trail from the server's packed double[] array. */
    private void drawRemoteTrail(GraphicsContext gc, double[] packed, Color color) {
        if (packed == null || packed.length < 4)
            return;
        gc.save();
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        gc.setGlobalAlpha(0.5);
        gc.setStroke(color);
        gc.setLineWidth(28.0);
        gc.beginPath();
        gc.moveTo(packed[0], packed[1]);
        for (int i = 2; i < packed.length - 1; i += 2) {
            gc.lineTo(packed[i], packed[i + 1]);
        }
        gc.stroke();
        gc.restore();
    }

    /**
     * True if the local player's head is within collision distance of an enemy
     * trail.
     */
    private boolean checkEnemyTrailCollision(double px, double py, double[] packed) {
        final double RADIUS = 14.0;
        Point2D head = new Point2D(px, py);
        for (int i = 0; i < packed.length - 3; i += 2) {
            Point2D a = new Point2D(packed[i], packed[i + 1]);
            Point2D b = new Point2D(packed[i + 2], packed[i + 3]);
            double dx = b.getX() - a.getX(), dy = b.getY() - a.getY();
            double lenSq = dx * dx + dy * dy;
            double t = lenSq == 0 ? 0
                    : Math.max(0, Math.min(1,
                            ((px - a.getX()) * dx + (py - a.getY()) * dy) / lenSq));
            Point2D proj = new Point2D(a.getX() + t * dx, a.getY() + t * dy);
            if (head.distance(proj) < RADIUS)
                return true;
        }
        return false;
    }

    /**
     * Creates an ImageView for a remote player sprite using the color-matched dough
     * image.
     */
    private ImageView createRemoteSprite(String colorHex) {
        String[] names = { "orange", "blue", "green", "red", "yellow", "pink", "purple", "indigo" };
        String dough = names[Math.abs(colorHex.hashCode()) % names.length];
        File imgFile = new File("assets/images/PlayersDough/" + dough + ".png");
        ImageView iv = new ImageView();
        if (imgFile.exists()) {
            iv.setImage(UIUtils.ImageCache.get("assets/images/PlayersDough/" + dough + ".png"));
        }
        iv.setFitWidth(100);
        iv.setFitHeight(100);
        iv.setPreserveRatio(true);
        return iv;
    }

    /** Removes all visual elements for a player that has died or disconnected. */
    private void appendChatMessage(String name, String text, String colorHex) {
        if (chatMessageArea.getChildren().size() > 0 && 
            chatMessageArea.getChildren().get(0) instanceof Label &&
            ((Label) chatMessageArea.getChildren().get(0)).getText().equals("No messages yet")) {
            chatMessageArea.getChildren().clear();
        }

        javafx.scene.text.Text nameText = new javafx.scene.text.Text("[" + name + "]: ");
        nameText.setFont(FONT_CHAT);
        try {
            if (colorHex != null && !colorHex.startsWith("#")) colorHex = "#" + colorHex;
            nameText.setFill(Color.web(colorHex != null ? colorHex : "#FFFFFF"));
        } catch (Exception e) {
            nameText.setFill(Color.WHITE);
        }

        javafx.scene.text.Text msgText = new javafx.scene.text.Text(text);
        msgText.setFont(FONT_CHAT);
        msgText.setFill(Color.WHITE);

        javafx.scene.text.TextFlow messageFlow = new javafx.scene.text.TextFlow(nameText, msgText);
        messageFlow.setPadding(new Insets(2, 0, 2, 0));

        chatMessageArea.getChildren().add(messageFlow);

        if (chatMessageArea.getChildren().size() > 50) {
            chatMessageArea.getChildren().remove(0);
        }

        // Auto-scroll to bottom
        Platform.runLater(() -> chatScroll.setVvalue(1.0));
    }

    /**
     * Called when the server notifies us that a remote player died.
     * After removing them, if we are the only player left alive we trigger
     * an immediate game-over (last player standing wins).
     */
    private void removeRemotePlayer(int playerId) {
        ImageView sprite = remoteSprites.remove(playerId);
        if (sprite != null)
            world.getChildren().remove(sprite);

        Canvas overlay = remoteOverlays.remove(playerId);
        if (overlay != null)
            world.getChildren().remove(overlay);

        remoteTrails.remove(playerId);
        remoteColors.remove(playerId);
        remoteTerritoryPercents.remove(playerId);
        remotePlayerNames.remove(playerId);
    }

    /**
     * Shows the game-over screen with the server-provided (or locally-built)
     * leaderboard.
     * Safe to call after gameClient.disconnect() has already been invoked.
     * Used in multiplayer mode instead of the single-player GameOverModal.
     */
    private void showMultiplayerGameOver(java.util.List<app.network.NetworkMessage.GameResult> results) {
        gameTimer.stop();
        gameLoop.stop();
        // Disconnect only if still connected (handleDeath may have already done this)
        if (gameClient != null && gameClient.isConnected())
            gameClient.disconnect();

        // Determine how the game ended:
        // LAST_STANDING if exactly one player has territory > 0, meaning all others
        // have died (including the local player who just died at 0%).
        long withTerritory = results.stream().filter(r -> r.territoryPercent > 0).count();
        boolean lastStanding = (withTerritory <= 1);
        GameOverModal.EndReason reason = lastStanding
                ? GameOverModal.EndReason.LAST_STANDING
                : GameOverModal.EndReason.TIMER;

        String spritePath = "assets/images/PlayersDough/" + chosenDough + ".png";
        GameOverModal modal = new GameOverModal(mainApp, results, myPlayerId, spritePath, reason);
        modal.show();
    }

    // -----------------------------------------------------------------------
    // Public
    // -----------------------------------------------------------------------

    public javafx.scene.Parent getRoot() {
        return root;
    }
}