package app;

import app.network.GameClient;
import app.screens.DevelopersPage;
import app.screens.GamePlayScreen;
import app.screens.LandingPageScreen;
import app.screens.MultiplayerScreen;
import app.screens.RulesPage;
import app.screens.SinglePlayerScreen;
import app.utils.UIUtils;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    private Stage  window;
    private javafx.scene.Scene mainScene;

    // Screen instances
    private LandingPageScreen landingPage;
    private SinglePlayerScreen singlePlayer;
    private MultiplayerScreen multiplayer;
    private DevelopersPage developersPage;
    private RulesPage rulesPage;

    @Override
    public void start(Stage primaryStage) {
        this.window = primaryStage;
        window.setTitle("137 Project - The Tray");

        // Kick off background image loading before any screen is constructed
        UIUtils.ImageCache.preload();

        landingPage  = new LandingPageScreen(this);
        singlePlayer = new SinglePlayerScreen(this);
        multiplayer  = new MultiplayerScreen(this);
        developersPage = new DevelopersPage(this);
        rulesPage = new RulesPage(this);

        mainScene = new javafx.scene.Scene(new javafx.scene.layout.Pane(), 1024, 768);
        mainScene.getStylesheets().add(new java.io.File("assets/css/app.css").toURI().toString());
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

    /** Launches the single-player game (random spawn, chosen color). */
    public void showGamePlay(String chosenDough) {
        GamePlayScreen screen = new GamePlayScreen(this, chosenDough);
        mainScene.setRoot(screen.getRoot());
        screen.getRoot().requestFocus();
    }

    public void showDevelopers() {
        mainScene.setRoot(developersPage.getRoot());
    }

    public void showRules() {
        mainScene.setRoot(rulesPage.getRoot());
    }

    public void showMultiplayerGame(GameClient client, double spawnX, double spawnY, String colorHex, int myPlayerId) {
        GamePlayScreen screen = new GamePlayScreen(this, client, spawnX, spawnY, colorHex, myPlayerId);
        mainScene.setRoot(screen.getRoot());
        screen.getRoot().requestFocus();
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    public javafx.stage.Stage getPrimaryStage() {
        return window;
    }

    public void setBackgroundBlur(boolean apply) {
        // Disabled because applying a GaussianBlur effect to the 3000x3000 GamePlayScreen root 
        // requires a massive RTTexture allocation which crashes Direct3D with an out-of-VRAM NPE.
        /*
        if (apply) {
            GaussianBlur blur = new GaussianBlur(15);
            ColorAdjust  darken = new ColorAdjust();
            darken.setBrightness(-0.5);
            blur.setInput(darken);
            window.getScene().getRoot().setEffect(blur);
        } else {
            window.getScene().getRoot().setEffect(null);
        }
        */
    }

    public void exitGame() {
        window.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}