package app.screens;

import java.io.File;

import app.Main;
import app.game_logic.TerritoryManager;
import app.game_logic.TrailManager;
import javafx.animation.AnimationTimer;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class GamePlayScreen {
    private Pane root;
    private Pane world;
    private Main mainApp;
    private TrailManager trailManager;
    private TerritoryManager territoryManager;

    private javafx.scene.canvas.Canvas trailCanvas;
    private javafx.scene.canvas.GraphicsContext trailGc;

    private ImageView playerSprite;
    private double playerX = 0;
    private double playerY = 0;
    private double speed = 1.5;

    private final double WORLD_RADIUS = 1500;

    private boolean upPressed, downPressed, leftPressed, rightPressed;

    private Label territoryLabel;
    private ProgressBar powerUpBar;

    private int ownedHexCount = 0;
    private int totalHexCount = 1000;

    private boolean powerUpActive = false;
    private double timeLeft = 0;
    private double totalDuration = 5.0;

    private double dirX = 1;
    private double dirY = 0;

    public GamePlayScreen(Main mainApp) {
        this.mainApp = mainApp;
        root = new Pane();

        File bgFile = new File("assets/images/GameplayBackground.jpg");
        if (bgFile.exists()) {
            Image bgImage = new Image(bgFile.toURI().toString());
            javafx.scene.layout.BackgroundImage background =
                    new javafx.scene.layout.BackgroundImage(
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

        world = new Pane();
        root.getChildren().add(world);

        Circle arena = new Circle(0, 0, WORLD_RADIUS);
        arena.setFill(Color.web("#f8f1df", 0.8));
        arena.setStroke(Color.WHITE);
        arena.setStrokeWidth(10);
        world.getChildren().add(arena);

        javafx.scene.canvas.Canvas hexCanvas = new javafx.scene.canvas.Canvas(WORLD_RADIUS * 2, WORLD_RADIUS * 2);
        hexCanvas.setTranslateX(-WORLD_RADIUS);
        hexCanvas.setTranslateY(-WORLD_RADIUS);

        javafx.scene.canvas.GraphicsContext gc = hexCanvas.getGraphicsContext2D();
        gc.setStroke(Color.web("#e0d5ba"));
        gc.setLineWidth(4);

        double hexRadius = 27.5;
        double hexW = Math.sqrt(3) * hexRadius;
        double hexH = 2 * hexRadius;

        for (double y = 0; y < WORLD_RADIUS * 2 + hexH; y += hexH * 0.75) {
            int row = (int) (y / (hexH * 0.75));
            double xOffset = (row % 2 == 0) ? 0 : hexW / 2;

            for (double x = 0; x < WORLD_RADIUS * 2 + hexW; x += hexW) {
                double centerX = x + xOffset;
                double centerY = y;

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

        world.getChildren().add(hexCanvas);

        trailManager = new TrailManager();
        territoryManager = new TerritoryManager();

        trailCanvas = new javafx.scene.canvas.Canvas(WORLD_RADIUS * 2, WORLD_RADIUS * 2);
        trailCanvas.setTranslateX(-WORLD_RADIUS);
        trailCanvas.setTranslateY(-WORLD_RADIUS);
        trailGc = trailCanvas.getGraphicsContext2D();
        world.getChildren().add(trailCanvas);

        File playerFile = new File("assets/images/PlayersDough/orange.png");
        if (playerFile.exists()) {
            Image playerImage = new Image(playerFile.toURI().toString());
            playerSprite = new ImageView(playerImage);
            playerSprite.setFitWidth(60);
            playerSprite.setFitHeight(60);
            world.getChildren().add(playerSprite);
        }

        Label instructions = new Label("Use WASD/Arrows to move. Press ESC to exit.");
        instructions.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");
        instructions.setLayoutX(10);
        instructions.setLayoutY(10);
        root.getChildren().add(instructions);

        territoryLabel = new Label("Territory: 0.0%");
        territoryLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");
        root.getChildren().add(territoryLabel);

        powerUpBar = new ProgressBar(0);
        powerUpBar.setPrefWidth(300);
        root.getChildren().add(powerUpBar);

        root.setFocusTraversable(true);

        // ✅ ONLY CHANGE: mouse controls direction (no movement logic touched)
        root.setOnMouseMoved(e -> {
            double dx = e.getX() - (root.getWidth() / 2);
            double dy = e.getY() - (root.getHeight() / 2);

            double len = Math.sqrt(dx * dx + dy * dy);
            if (len != 0) {
                dirX = dx / len;
                dirY = dy / len;
            }
        });

        root.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.W || e.getCode() == KeyCode.UP) { dirX = 0; dirY = -1; }
            if (e.getCode() == KeyCode.S || e.getCode() == KeyCode.DOWN) { dirX = 0; dirY = 1; }
            if (e.getCode() == KeyCode.A || e.getCode() == KeyCode.LEFT) { dirX = -1; dirY = 0; }
            if (e.getCode() == KeyCode.D || e.getCode() == KeyCode.RIGHT) { dirX = 1; dirY = 0; }

            if (e.getCode() == KeyCode.ESCAPE) mainApp.showLandingPage();
        });

        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
            }
        };
        gameLoop.start();
    }

    private void update() {
        if (playerSprite == null) return;

        double screenWidth = root.getWidth();
        double screenHeight = root.getHeight();
        if (screenWidth == 0) return;

        // ✅ ONLY CHANGE: ALWAYS MOVE
        playerX += dirX * speed;
        playerY += dirY * speed;

        double distance = Math.sqrt(playerX * playerX + playerY * playerY);
        double maxRadius = WORLD_RADIUS - 15;

        if (distance > maxRadius) {
            double angle = Math.atan2(playerY, playerX);
            playerX = Math.cos(angle) * maxRadius;
            playerY = Math.sin(angle) * maxRadius;
        }

        playerSprite.setX(playerX - 30);
        playerSprite.setY(playerY - 30);

        territoryManager.claimTile(playerX, playerY);
        ownedHexCount = territoryManager.getOwnedHexCount();

        trailManager.updateTrail(playerX, playerY, false);

        trailGc.clearRect(0, 0, trailCanvas.getWidth(), trailCanvas.getHeight());
        trailGc.save();
        trailGc.translate(WORLD_RADIUS, WORLD_RADIUS);
        territoryManager.drawTerritory(trailGc, Color.web("#FFA500", 0.6));
        trailGc.restore();

        if (trailManager.checkSelfCollision(playerX, playerY)) {
            System.out.println("BOOM! You hit your own dough!");

            trailManager.updateTrail(playerX, playerY, true);
            territoryManager.clearTerritory();

            ownedHexCount = 0;
            playerX = 0;
            playerY = 0;
        }

        world.setTranslateX((screenWidth / 2) - playerX);
        world.setTranslateY((screenHeight / 2) - playerY);

        territoryLabel.setLayoutX(screenWidth - 220);
        territoryLabel.setLayoutY(20);

        powerUpBar.setLayoutX((screenWidth / 2) - 150);
        powerUpBar.setLayoutY(screenHeight - 50);

        double ownedPercent = (double) ownedHexCount / totalHexCount * 100;
        territoryLabel.setText(String.format("Territory: %.1f%%", ownedPercent));
    }

    public javafx.scene.Parent getRoot() {
        return root;
    }
}