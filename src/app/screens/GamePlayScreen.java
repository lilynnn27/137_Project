package app.screens;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import app.Main;
import app.game_logic.TerritoryManager;
import app.game_logic.Timer;
import app.game_logic.TrailManager;
import javafx.animation.AnimationTimer;
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

    /** Canvas for territory fill + trail (redrawn every frame). */
    private final Canvas overlayCanvas;
    private final GraphicsContext overlayGc;

    /** Player visual */
    private ImageView playerSprite;

    /** Player world-space position */
    private double playerX = 0;
    private double playerY = 0;
    private final double SPEED = 2.5;

    /** Current movement direction (unit vector) */
    Set<KeyCode> pressedKeys = new HashSet<>();
    private double dirX = 1;
    private double dirY = 0;

    //** for smoothness when using keyboard keys */
    private double targetDirX = 0;
    private double targetDirY = 0;
    private final double TURN_SMOOTHNESS = 0.15;

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
            "red",    Color.web("#E53935"),
            "blue",   Color.web("#1E88E5"),
            "green",  Color.web("#43A047"),
            "yellow", Color.web("#FDD835"),
            "pink",   Color.web("#EC407A"),
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

    /** Named game loop so it can be stopped on game-over. */
    private AnimationTimer gameLoop;

    /** Countdown timer from develop branch. */
    private Timer gameTimer;

    // -----------------------------------------------------------------------
    // Constructor / Setup
    // -----------------------------------------------------------------------

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
        String chosenDough = doughNames[new Random().nextInt(doughNames.length)];
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
                10, // seconds (set to desired game duration)
                () -> javafx.application.Platform.runLater(
                        () -> timerLabel.setText("Time: " + gameTimer.getFormattedTime())),
                () -> {
                    System.out.println("Timer reached zero!");
                    javafx.application.Platform.runLater(this::showGameOver);
                });
        gameTimer.start();

        // --- Input ---
        root.setFocusTraversable(true);

        root.setOnMouseMoved(e -> {
            double dx = e.getX() - (root.getWidth() / 2);
            double dy = e.getY() - (root.getHeight() / 2);
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len > 1) {
                dirX = dx / len;
                dirY = dy / len;
            }
        });

        // key handling
        root.setOnKeyPressed(e -> {
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
                update();
            }
        };
        gameLoop.start();
    }

    // -----------------------------------------------------------------------
    // Game loop
    // -----------------------------------------------------------------------

    private void update() {
        double screenW = root.getWidth();
        double screenH = root.getHeight();
        if (screenW == 0)
            return;

        //Smooth Direction
        dirX += (targetDirX - dirX) * TURN_SMOOTHNESS;
        dirY += (targetDirY - dirY) * TURN_SMOOTHNESS;

        // re-normalize to keep constant speed
        double len = Math.sqrt(dirX * dirX + dirY * dirY);
        if (len > 0) {
            dirX /= len;
            dirY /= len;
        }

        // --- Move player ---
        playerX += dirX * SPEED;
        playerY += dirY * SPEED;

        // Clamp inside arena
        double dist = Math.sqrt(playerX * playerX + playerY * playerY);
        double maxR = WORLD_RADIUS - 30;
        if (dist > maxR) {
            double angle = Math.atan2(playerY, playerX);
            playerX = Math.cos(angle) * maxR;
            playerY = Math.sin(angle) * maxR;
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

        // --- Update sprite position ---
        if (playerSprite != null) {
            playerSprite.setX(playerX - 30);
            playerSprite.setY(playerY - 30);
        }

        // --- Render overlay (territory + trail) ---
        overlayGc.clearRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());
        overlayGc.save();
        overlayGc.translate(WORLD_RADIUS, WORLD_RADIUS);
        territoryManager.drawTerritory(overlayGc, PLAYER_COLOR);
        trailManager.draw(overlayGc, PLAYER_COLOR);
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
    }

    // -----------------------------------------------------------------------
    // Death
    // -----------------------------------------------------------------------

    private void handleDeath() {
        System.out.println("You hit your own trail — game restarting!");
        trailManager.clear();
        territoryManager.clearTerritory();

        playerX = 0;
        playerY = 0;
        dirX = 1;
        dirY = 0;
        outsideTerritory = false;
        territoryManager.initStartingTerritory(playerX, playerY, 70);
    }

    // -----------------------------------------------------------------------
    // Game Over
    // -----------------------------------------------------------------------

    private void showGameOver() {
        gameTimer.stop();
        gameLoop.stop();

        javafx.application.Platform.runLater(() -> {
            GameOverModal modal = new GameOverModal(mainApp, ownedHexCount, totalHexCount);
            modal.show();
        });
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
    // Public
    // -----------------------------------------------------------------------

    public javafx.scene.Parent getRoot() {
        return root;
    }
}