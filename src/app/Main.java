package app;

import app.screens.LandingPageScreen;
import app.screens.SinglePlayerScreen;
import app.screens.MultiplayerScreen;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    private Stage window;
    
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

        // Start on the Landing Page
        showLandingPage();
        
        window.show();
    }

    // Methods to switch screens
    public void showLandingPage() {
        window.setScene(landingPage.getScene());
    }

    public void showSinglePlayer() {
        window.setScene(singlePlayer.getScene());
    }

    public void showMultiplayer() {
        window.setScene(multiplayer.getScene());
    }
    
    public void exitGame() {
        window.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
