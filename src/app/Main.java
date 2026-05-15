package app;

import app.network.GameClient;
import app.screens.GamePlayScreen;
import app.screens.LandingPageScreen;
import app.screens.MultiplayerScreen;
import app.screens.SinglePlayerScreen;
import app.utils.UIUtils;
import javafx.application.Application;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.stage.Stage;

public class Main extends Application {

    private Stage  window;
    private javafx.scene.Scene mainScene;

    // Screen instances
    private LandingPageScreen  landingPage;
    private SinglePlayerScreen singlePlayer;
    private MultiplayerScreen  multiplayer;

    @Override
    public void start(Stage primaryStage) {
        this.window = primaryStage;
        window.setTitle("137 Project - The Tray");

        // Kick off background image loading before any screen is constructed
        UIUtils.ImageCache.preload();

        landingPage  = new LandingPageScreen(this);
        singlePlayer = new SinglePlayerScreen(this);
        multiplayer  = new MultiplayerScreen(this);

        mainScene = new javafx.scene.Scene(new javafx.scene.layout.Pane(), 1024, 768);
        window.setScene(mainScene);

        showLandingPage();

        window.setMaximized(true);
        window.show();
    }

    // ── Screen transitions ────────────────────────────────────────────────

    public void showLandingPage() {
        mainScene.setRoot(landingPage.getRoot());
    }

    public void showSinglePlayer() {
        mainScene.setRoot(singlePlayer.getRoot());
    }

    public void showMultiplayer() {
        // Re-create so lobby state is fresh each visit
        multiplayer = new MultiplayerScreen(this);
        mainScene.setRoot(multiplayer.getRoot());
    }

    /** Launches the single-player game (random spawn, random color). */
    public void showGamePlay() {
        GamePlayScreen screen = new GamePlayScreen(this);
        mainScene.setRoot(screen.getRoot());
        screen.getRoot().requestFocus();
    }

    /**
     * Launches the multiplayer game screen for this client.
     *
     * @param client     The connected {@link GameClient} (already joined).
     * @param spawnX     World-space spawn X assigned by the server.
     * @param spawnY     World-space spawn Y assigned by the server.
     * @param colorHex   CSS hex color assigned by the server (e.g. "#FF7043").
     * @param myPlayerId This client's player ID.
     */
    public void showMultiplayerGame(GameClient client,
                                    double spawnX, double spawnY,
                                    String colorHex, int myPlayerId) {
        GamePlayScreen screen = new GamePlayScreen(this, client, spawnX, spawnY, colorHex, myPlayerId);
        mainScene.setRoot(screen.getRoot());
        screen.getRoot().requestFocus();
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    public javafx.stage.Stage getPrimaryStage() {
        return window;
    }

    public void setBackgroundBlur(boolean apply) {
        if (apply) {
            GaussianBlur blur = new GaussianBlur(15);
            ColorAdjust  darken = new ColorAdjust();
            darken.setBrightness(-0.5);
            blur.setInput(darken);
            window.getScene().getRoot().setEffect(blur);
        } else {
            window.getScene().getRoot().setEffect(null);
        }
    }

    public void exitGame() {
        window.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}