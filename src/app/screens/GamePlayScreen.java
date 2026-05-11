package app.screens;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import app.Main;
import app.game_logic.FreezeHazard;
import app.game_logic.PickupEntity;
import app.game_logic.ReverseControlsHazard;
import app.game_logic.SlowingHazard;
import app.game_logic.TerritoryManager;
import app.game_logic.Timer;
import app.game_logic.TrailManager;
import app.network.GameClient;
import app.network.NetworkMessage.PlayerState;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class GamePlayScreen {

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private final Pane root;
    private final Pane world;
    private final Main mainApp;

    private final TrailManager trailManager = new TrailManager();
    private final TerritoryManager territoryManager = new TerritoryManager();
    /**
     * Other players' trail managers — populated by multiplayer. Empty in
     * single-player.
     */
    private final List<TrailManager> enemyTrailManagers = new ArrayList<>();

    /** Canvas for territory fill + trail (redrawn every frame). */
    private final Canvas overlayCanvas;
    private final GraphicsContext overlayGc;

    /** Player visual */
    private ImageView playerSprite;

    // ---- Multiplayer networking ----
    /** Non-null when running in multiplayer mode. */
    private GameClient gameClient;
    /** This client's player ID (assigned by server). -1 in single-player. */
    private int myPlayerId = -1;
    /**
     * Canvases for remote players' territory + trails.
     * One entry per remote player, keyed by their playerId.
     */
    private final java.util.Map<Integer, Canvas>       remoteOverlays  = new java.util.LinkedHashMap<>();
    private final java.util.Map<Integer, ImageView>    remoteSprites   = new java.util.LinkedHashMap<>();
    private final java.util.Map<Integer, TrailManager> remoteTrails    = new java.util.LinkedHashMap<>();
    private final java.util.Map<Integer, TerritoryManager> remoteTerritories = new java.util.LinkedHashMap<>();
    private final java.util.Map<Integer, Color>        remoteColors    = new java.util.LinkedHashMap<>();
    /** How many frames to skip between network sends (send every Nth frame). */
    private static final int NET_SEND_INTERVAL = 3;
    private int netFrameCounter = 0;

    /** Player world-space position */
    private double playerX = 0;
    private double playerY = 0;
    private final double SPEED = 2.5;

    /**
     * Multiplier applied to SPEED. Modified by hazards (e.g. SlowingHazard = 0.70).
     */
    private double speedMultiplier = 1.0;
    /**
     * Nanosecond timestamp when the current speed effect should expire (0 = no
     * effect).
     */
    private long speedEffectEndNanos = 0;

    /** Current movement direction (unit vector) */
    Set<KeyCode> pressedKeys = new HashSet<>();
    private double dirX = 1;
    private double dirY = 0;
    private double lastDirX = 1;
    private double lastDirY = 0;

    // ** for smoothness when using keyboard keys */
    private double targetDirX = 1;
    private double targetDirY = 0;
    private final double TURN_SMOOTHNESS = 0.15;

    private enum InputMode {
        KEYBOARD, MOUSE
    }

    private InputMode activeInputMode = InputMode.KEYBOARD;

    /** World radius */
    private final double WORLD_RADIUS = 1500;

    /** Player dough color — derived from the randomly chosen dough sprite. */
    private final Color PLAYER_COLOR;

    /**
     * Maps each dough filename (without extension) to its representative color.
     * Colors are picked to closely match the actual sprite tones.
     */
    private static final Map<String, Color> DOUGH_COLORS = Map.of(
            "orange", Color.web("#FF7043"),
            "red", Color.web("#E53935"),
            "blue", Color.web("#1E88E5"),
            "green", Color.web("#43A047"),
            "yellow", Color.web("#FDD835"),
            "pink", Color.web("#EC407A"),
            "purple", Color.web("#8E24AA"),
            "indigo", Color.web("#3949AB"));

    /** HUD labels */
    private final Label territoryLabel;
    private final ProgressBar powerUpBar;
    private Label timerLabel;

    /** Hex ownership counters (used by GameOverModal). */
    private int ownedHexCount = 0;
    private final int totalHexCount = 1000;

    /** True while the player is outside their territory (trail is active). */
    private boolean outsideTerritory = false;

    /** True once any death condition has fired — prevents double-invocation. */
    private boolean isDead = false;

    /**
     * Active pickups on the map (hazards + power-ups). Despawned entries are
     * removed each frame.
     */
    private final List<PickupEntity> pickups = new ArrayList<>();
    /** Sprite for H1 Rolling Pin hazard; null if the file is missing. */
    private Image rollingPinSprite;
    /** Nanosecond timestamp of the last Rolling Pin spawn (0 = none yet). */
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
    /** Nanosecond timestamp of the last Rotten Egg spawn (0 = none yet). */
    private long lastRottenEggSpawnNanos = 0;
    /** How often to spawn a new Rotten Egg hazard (15 seconds). */
    private static final long ROTTEN_EGG_SPAWN_INTERVAL_NANOS = 15_000_000_000L;

    /** Shared RNG — used for dough selection and hazard spawning. */
    private final Random rng = new Random();

    /** Named game loop so it can be stopped on game-over. */
    private AnimationTimer gameLoop;

    /** Countdown timer from develop branch. */
    private Timer gameTimer;

    // -----------------------------------------------------------------------
    // Constructor / Setup
    // -----------------------------------------------------------------------

    /**
     * Multiplayer constructor — spawns at server-assigned position with a
     * fixed color, and sends position updates to the server each frame.
     *
     * @param mainApp    The main application.
     * @param client     Already-connected {@link GameClient}.
     * @param spawnX     World-space X assigned by the server.
     * @param spawnY     World-space Y assigned by the server.
     * @param colorHex   CSS color string (e.g. "#FF7043").
     * @param myPlayerId This client's ID.
     */
    public GamePlayScreen(Main mainApp, GameClient client,
                          double spawnX, double spawnY,
                          String colorHex, int myPlayerId) {
        this(mainApp); // Runs the full single-player setup first

        // Override defaults set by the single-player constructor
        this.gameClient  = client;
        this.myPlayerId  = myPlayerId;

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

        // Register the GAME_STATE callback — runs on network thread, touch UI on FX thread
        client.onGameState(states -> Platform.runLater(() -> applyRemoteStates(states)));
        client.onPlayerDied(deadId -> Platform.runLater(() -> removeRemotePlayer(deadId)));
        client.onGameOver(results -> Platform.runLater(() -> {
            if (!isDead) {
                isDead = true;
                showMultiplayerGameOver(results);
            }
        }));
    }

    /** Single-player constructor (original). */
    public GamePlayScreen(Main mainApp) {
        this.mainApp = mainApp;

        root = new Pane();

        // --- Background ---
        File bgFile = new File("assets/images/GameplayBackground.jpg");
        if (bgFile.exists()) {
            Image bgImage = new Image(bgFile.toURI().toString());
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

        // --- Player sprite: pick a random dough at each game start ---
        String[] doughNames = { "orange", "red", "blue", "green", "yellow", "pink", "purple", "indigo" };
        String chosenDough = doughNames[rng.nextInt(doughNames.length)];
        PLAYER_COLOR = DOUGH_COLORS.getOrDefault(chosenDough, Color.web("#FF7043"));

        File playerFile = new File("assets/images/PlayersDough/" + chosenDough + ".png");
        if (playerFile.exists()) {
            Image playerImage = new Image(playerFile.toURI().toString());
            playerSprite = new ImageView(playerImage);
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
            rollingPinSprite = new Image(rpFile.toURI().toString());
        }

        // --- H2 Ice Spill hazard sprite ---
        File iceFile = new File("assets/images/hazard/Ice-Hazard.png");
        if (iceFile.exists()) {
            iceSprite = new Image(iceFile.toURI().toString());
        }

        // --- H3 Rotten Egg hazard sprite ---
        File reFile = new File("assets/images/hazard/RottenEgg-Hazard.png");
        if (reFile.exists()) {
            rottenEggSprite = new Image(reFile.toURI().toString());
        }

        // --- Starting territory centred on spawn ---
        territoryManager.initStartingTerritory(playerX, playerY, 70);

        // --- HUD ---
        Label instructions = new Label("WASD / Arrows to move · ESC to exit");
        instructions.setStyle(
                "-fx-text-fill: white; -fx-font-size: 18px; -fx-effect: dropshadow(gaussian,black,4,0.6,0,0);");
        instructions.setLayoutX(10);
        instructions.setLayoutY(10);
        root.getChildren().add(instructions);

        territoryLabel = new Label("Territory: 0.0%");
        territoryLabel.setStyle(
                "-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian,black,4,0.6,0,0);");
        root.getChildren().add(territoryLabel);

        powerUpBar = new ProgressBar(0);
        powerUpBar.setPrefWidth(300);
        root.getChildren().add(powerUpBar);

        // --- Timer HUD (from develop branch) ---
        timerLabel = new Label("Time: 00:00");
        timerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");
        timerLabel.setLayoutX(20);
        timerLabel.setLayoutY(50);
        root.getChildren().add(timerLabel);

        gameTimer = new Timer(
                40, // seconds (set to desired game duration)
                () -> javafx.application.Platform.runLater(
                        () -> timerLabel.setText("Time: " + gameTimer.getFormattedTime())),
                () -> {
                    System.out.println("Timer reached zero!");
                    if (!isDead) {
                        isDead = true;
                        showGameOver();
                    }
                });
        gameTimer.start();

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
            activeInputMode = InputMode.KEYBOARD;
            pressedKeys.add(e.getCode());
            updateDirection();

            if (e.getCode() == KeyCode.ESCAPE) {
                mainApp.showLandingPage();
            }
        });

        root.setOnKeyReleased(e -> {
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

        // --- Render overlay (territory + trail) ---
        overlayGc.clearRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());
        overlayGc.save();
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

        // --- HUD ---
        double areaFraction = territoryManager.getApproximateAreaFraction(Math.PI * WORLD_RADIUS * WORLD_RADIUS);
        ownedHexCount = (int) (areaFraction * totalHexCount);
        territoryLabel.setText(String.format("Territory: %.1f%%", areaFraction * 100));
        territoryLabel.setLayoutX(screenW - 230);
        territoryLabel.setLayoutY(20);

        powerUpBar.setLayoutX((screenW / 2) - 150);
        powerUpBar.setLayoutY(screenH - 50);

        // --- Multiplayer: send position to server every NET_SEND_INTERVAL frames ---
        if (gameClient != null && gameClient.isConnected()) {
            netFrameCounter++;
            if (netFrameCounter >= NET_SEND_INTERVAL) {
                netFrameCounter = 0;
                List<Point2D> trailPts = trailManager.getTrailPoints();
                double[] packed = new double[trailPts.size() * 2];
                for (int i = 0; i < trailPts.size(); i++) {
                    packed[i * 2]     = trailPts.get(i).getX();
                    packed[i * 2 + 1] = trailPts.get(i).getY();
                }
                gameClient.sendPositionUpdate(playerX, playerY, dirX, dirY, packed, areaFraction * 100);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Hazard spawning
    // -----------------------------------------------------------------------

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

    /** Spawns a Rotten Egg (H3) hazard at a random arena position outside territory. */
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
        showGameOver();
    }

    // -----------------------------------------------------------------------
    // Game Over
    // -----------------------------------------------------------------------

    private void showGameOver() {
        gameTimer.stop();
        gameLoop.stop();

        GameOverModal modal = new GameOverModal(mainApp, ownedHexCount, totalHexCount);
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
            if (state.playerId == myPlayerId) continue; // skip self
            if (state.isDead) {
                removeRemotePlayer(state.playerId);
                continue;
            }

            Color color = remoteColors.computeIfAbsent(state.playerId,
                id -> Color.web(state.colorHex));

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
            GraphicsContext gc = overlay.getGraphicsContext2D();
            gc.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());
            gc.save();
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
        if (packed == null || packed.length < 4) return;
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

    /** True if the local player's head is within collision distance of an enemy trail. */
    private boolean checkEnemyTrailCollision(double px, double py, double[] packed) {
        final double RADIUS = 14.0;
        Point2D head = new Point2D(px, py);
        for (int i = 0; i < packed.length - 3; i += 2) {
            Point2D a = new Point2D(packed[i],     packed[i + 1]);
            Point2D b = new Point2D(packed[i + 2], packed[i + 3]);
            double dx = b.getX() - a.getX(), dy = b.getY() - a.getY();
            double lenSq = dx * dx + dy * dy;
            double t = lenSq == 0 ? 0 : Math.max(0, Math.min(1,
                ((px - a.getX()) * dx + (py - a.getY()) * dy) / lenSq));
            Point2D proj = new Point2D(a.getX() + t * dx, a.getY() + t * dy);
            if (head.distance(proj) < RADIUS) return true;
        }
        return false;
    }

    /** Creates an ImageView for a remote player sprite using the color-matched dough image. */
    private ImageView createRemoteSprite(String colorHex) {
        String[] names  = { "orange", "blue", "green", "red", "yellow", "pink", "purple", "indigo" };
        String dough    = names[Math.abs(colorHex.hashCode()) % names.length];
        File   imgFile  = new File("assets/images/PlayersDough/" + dough + ".png");
        ImageView iv    = new ImageView();
        if (imgFile.exists()) {
            iv.setImage(new Image(imgFile.toURI().toString()));
        }
        iv.setFitWidth(100);
        iv.setFitHeight(100);
        iv.setPreserveRatio(true);
        return iv;
    }

    /** Removes all visual elements for a player that has died or disconnected. */
    private void removeRemotePlayer(int playerId) {
        ImageView sprite = remoteSprites.remove(playerId);
        if (sprite != null) world.getChildren().remove(sprite);

        Canvas overlay = remoteOverlays.remove(playerId);
        if (overlay != null) world.getChildren().remove(overlay);

        remoteTrails.remove(playerId);
        remoteColors.remove(playerId);
    }

    /**
     * Shows the game-over screen with the server-provided leaderboard.
     * Used in multiplayer mode instead of the single-player GameOverModal.
     */
    private void showMultiplayerGameOver(java.util.List<app.network.NetworkMessage.GameResult> results) {
        gameTimer.stop();
        gameLoop.stop();
        if (gameClient != null) gameClient.disconnect();

        // Re-use GameOverModal but pass the top player's score for now.
        // You can extend GameOverModal later to show the full leaderboard.
        int myScore = 0;
        for (app.network.NetworkMessage.GameResult r : results) {
            if (r.playerId == myPlayerId) {
                myScore = (int) r.territoryPercent;
                break;
            }
        }
        GameOverModal modal = new GameOverModal(mainApp, myScore, 100);
        modal.show();
    }

    // -----------------------------------------------------------------------
    // Public
    // -----------------------------------------------------------------------

    public javafx.scene.Parent getRoot() {
        return root;
    }
}