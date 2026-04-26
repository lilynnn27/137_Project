package app.screens;

import app.Main;
import javafx.animation.AnimationTimer;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.io.File;

public class GamePlayScreen {
    private Pane root;
    private Pane world;
    private Main mainApp;

    // Game state (World coordinates)
    private ImageView playerSprite;
    private double playerX = 0;
    private double playerY = 0;
    private double speed = 5.0;

    // world radius
    private final double WORLD_RADIUS = 1500;

    // Input tracking
    private boolean upPressed, downPressed, leftPressed, rightPressed;

    public GamePlayScreen(Main mainApp) {
        this.mainApp = mainApp;
        root = new Pane();
        // Setup Static Screen Background (One big picture, no repeating)
        File bgFile = new File("assets/images/GameplayBackground.jpg");
        if (bgFile.exists()) {
            Image bgImage = new Image(bgFile.toURI().toString());
            // Cover the entire screen with one big static image
            javafx.scene.layout.BackgroundImage background = new javafx.scene.layout.BackgroundImage(
                    bgImage,
                    javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                    javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                    javafx.scene.layout.BackgroundPosition.CENTER,
                    new javafx.scene.layout.BackgroundSize(javafx.scene.layout.BackgroundSize.AUTO,
                            javafx.scene.layout.BackgroundSize.AUTO, false, false, false, true));
            root.setBackground(new javafx.scene.layout.Background(background));
        } else {
            root.setStyle("-fx-background-color: #111111;");
        }

        // The world pane holds everything that moves relative to the camera
        world = new Pane();
        root.getChildren().add(world);

        // Setup Circular Arena Map
        // Centered at (0,0) in world space
        Circle arena = new Circle(0, 0, WORLD_RADIUS);
        // Fill the inside of the arena with 20% transparency
        arena.setFill(Color.web("#f8f1df", 0.8));

        // border for the edge of the world
        arena.setStroke(Color.WHITE);
        arena.setStrokeWidth(10);
        world.getChildren().add(arena);

        // Hexagon Grid Overlay using a fast Canvas
        javafx.scene.canvas.Canvas hexCanvas = new javafx.scene.canvas.Canvas(WORLD_RADIUS * 2, WORLD_RADIUS * 2);
        hexCanvas.setTranslateX(-WORLD_RADIUS);
        hexCanvas.setTranslateY(-WORLD_RADIUS);
        javafx.scene.canvas.GraphicsContext gc = hexCanvas.getGraphicsContext2D();
        // darker beige for the honeycomb lines
        gc.setStroke(Color.web("#e0d5ba"));
        gc.setLineWidth(4);

        // Radius 27.5 makes the hexagon diameter 55 (approx same size as 60x60 sprite)
        double hexRadius = 27.5;
        double hexW = Math.sqrt(3) * hexRadius;
        double hexH = 2 * hexRadius;

        for (double y = 0; y < WORLD_RADIUS * 2 + hexH; y += hexH * 0.75) {
            int row = (int) (y / (hexH * 0.75));
            double xOffset = (row % 2 == 0) ? 0 : hexW / 2;
            for (double x = 0; x < WORLD_RADIUS * 2 + hexW; x += hexW) {
                double centerX = x + xOffset;
                double centerY = y;

                // Only draw if near the circle
                double dx = centerX - WORLD_RADIUS;
                double dy = centerY - WORLD_RADIUS;
                if (Math.sqrt(dx * dx + dy * dy) <= WORLD_RADIUS + hexRadius) {
                    double[] xPoints = new double[6];
                    double[] yPoints = new double[6];
                    for (int i = 0; i < 6; i++) {
                        double angle_rad = Math.PI / 180 * (60 * i - 30);
                        xPoints[i] = centerX + hexRadius * Math.cos(angle_rad);
                        yPoints[i] = centerY + hexRadius * Math.sin(angle_rad);
                    }
                    gc.strokePolygon(xPoints, yPoints, 6);
                }
            }
        }

        // Clip the canvas strictly to the circle so grid lines don't leak outside
        Circle clip = new Circle(WORLD_RADIUS, WORLD_RADIUS, WORLD_RADIUS);
        hexCanvas.setClip(clip);
        world.getChildren().add(hexCanvas);

        // Setup Player Sprite (Randomly chosen from the directory)
        File playerFile = new File("assets/images/PlayersDough/orange.png"); // Default fallback
        File doughDir = new File("assets/images/PlayersDough");
        if (doughDir.exists() && doughDir.isDirectory()) {
            File[] doughFiles = doughDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (doughFiles != null && doughFiles.length > 0) {
                int randomIndex = new java.util.Random().nextInt(doughFiles.length);
                playerFile = doughFiles[randomIndex];
            }
        }

        if (playerFile.exists()) {
            Image playerImage = new Image(playerFile.toURI().toString());
            playerSprite = new ImageView(playerImage);

            // Scale to 60x60 while keeping the correct aspect ratio
            playerSprite.setPreserveRatio(true);
            playerSprite.setFitWidth(60);
            playerSprite.setFitHeight(60);

            world.getChildren().add(playerSprite);
        }

        // UI Overlay
        javafx.scene.control.Label instructions = new javafx.scene.control.Label(
                "Use WASD/Arrows to move. Press ESC to exit.");
        instructions.setStyle(
                "-fx-text-fill: white; -fx-font-size: 20px; -fx-padding: 10; -fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 5;");
        instructions.setLayoutX(10);
        instructions.setLayoutY(10);
        root.getChildren().add(instructions);

        this.root = root;
        root.setFocusTraversable(true);
        root.setOnMouseClicked(event -> root.requestFocus());

        // Input Handling
        // WASD/Arrow keys for movement
        // ESC to exit
        root.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.W || e.getCode() == KeyCode.UP)
                upPressed = true;
            if (e.getCode() == KeyCode.S || e.getCode() == KeyCode.DOWN)
                downPressed = true;
            if (e.getCode() == KeyCode.A || e.getCode() == KeyCode.LEFT)
                leftPressed = true;
            if (e.getCode() == KeyCode.D || e.getCode() == KeyCode.RIGHT)
                rightPressed = true;
            if (e.getCode() == KeyCode.ESCAPE)
                mainApp.showLandingPage();
        });

        root.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.W || e.getCode() == KeyCode.UP)
                upPressed = false;
            if (e.getCode() == KeyCode.S || e.getCode() == KeyCode.DOWN)
                downPressed = false;
            if (e.getCode() == KeyCode.A || e.getCode() == KeyCode.LEFT)
                leftPressed = false;
            if (e.getCode() == KeyCode.D || e.getCode() == KeyCode.RIGHT)
                rightPressed = false;
        });

        // Start the Custom Game Loop
        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
            }
        };
        gameLoop.start();
    }

    private void update() {
        if (playerSprite == null)
            return;

        double screenWidth = root.getWidth();
        double screenHeight = root.getHeight();
        if (screenWidth == 0)
            return; // Wait for layout

        // Move player coordinates in the infinite world space
        if (upPressed)
            playerY -= speed;
        if (downPressed)
            playerY += speed;
        if (leftPressed)
            playerX -= speed;
        if (rightPressed)
            playerX += speed;

        // Collision detection against the giant world boundary
        // Distance from world center (0,0)
        double distance = Math.sqrt(playerX * playerX + playerY * playerY);

        // 15 padding for transparent pixels inside the 60x60 square
        double maxRadius = WORLD_RADIUS - 15;
        if (distance > maxRadius) {
            double angle = Math.atan2(playerY, playerX);
            playerX = Math.cos(angle) * maxRadius;
            playerY = Math.sin(angle) * maxRadius;
        }

        // Apply updated coordinates to the visual sprite
        playerSprite.setX(playerX - 30);
        playerSprite.setY(playerY - 30);

        // CAMERA LOGIC: Move the entire world so the player is always at the center of
        // the screen
        world.setTranslateX((screenWidth / 2) - playerX);
        world.setTranslateY((screenHeight / 2) - playerY);
    }

    public javafx.scene.Parent getRoot() {
        return root;
    }
}
