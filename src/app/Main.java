package app;

import app.screens.GamePlayScreen;
import app.screens.LandingPageScreen;
import app.screens.MultiplayerScreen;
import app.screens.SinglePlayerScreen;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    private Stage window;
    private javafx.scene.Scene mainScene;
    
    // Screen instances
    private LandingPageScreen landingPage;
    private SinglePlayerScreen singlePlayer;
    private MultiplayerScreen multiplayer;

    @Override
    public void start(Stage primaryStage) {
        this.window = primaryStage;
        window.setTitle("137 Project - Networked Game");

        // Initialize screens
        landingPage = new LandingPageScreen(this);
        singlePlayer = new SinglePlayerScreen(this);
        multiplayer = new MultiplayerScreen(this);

        mainScene = new javafx.scene.Scene(new javafx.scene.layout.Pane(), 1024, 768);
        window.setScene(mainScene);

        // Start on the Landing Page
        showLandingPage();
        
        window.setMaximized(true);
        window.show();
    }

    // Methods to switch screens
    public void showLandingPage() {
        mainScene.setRoot(landingPage.getRoot());
    }

    public void showSinglePlayer() {
        mainScene.setRoot(singlePlayer.getRoot());
    }

    public void showMultiplayer() {
        mainScene.setRoot(multiplayer.getRoot());
    }
    
    public javafx.stage.Stage getPrimaryStage(){
        return window;
    }

    public void showGamePlay(){
        GamePlayScreen gamePlayScreen = new GamePlayScreen(this);
        mainScene.setRoot(gamePlayScreen.getRoot());
        gamePlayScreen.getRoot().requestFocus();
    }
    
    public void exitGame() {
        window.close();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
