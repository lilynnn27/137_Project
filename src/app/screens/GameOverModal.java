package app.screens;

import java.io.File;

import app.Main;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class GameOverModal {

    private final Stage window;
    private final Main mainApp;
    private final int score;
    private final int total;

    public GameOverModal(Main mainApp, int score, int total) {
        this.mainApp = mainApp;
        this.score = score;
        this.total = total;

        window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.initStyle(StageStyle.TRANSPARENT); // Removes window borders
        
        window.initOwner(mainApp.getPrimaryStage()); 
    }

    public void show() {
        File file = new File("assets/images/GameOverModal.png");
        ImageView bgView = new ImageView();
        if (file.exists()) {
            bgView.setImage(new Image(file.toURI().toString()));
        }

        Label titleLabel = new Label("GAME OVER");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #5d4037;");

        double percent = ((double) score / total) * 100;
        Label scoreLabel = new Label(String.format("Final Territory: %.1f%%", percent));
        scoreLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #795548;");

        Button retryBtn = new Button("PLAY AGAIN");
        retryBtn.setOnAction(e -> {
            window.close();
            mainApp.showGamePlay(); // This triggers the fresh start in Main.java
        });

        Button menuBtn = new Button("BACK TO MENU");
        menuBtn.setOnAction(e -> {
            window.close();
            mainApp.showLandingPage();
        });

        VBox content = new VBox(20, titleLabel, scoreLabel, retryBtn, menuBtn);
        content.setAlignment(Pos.CENTER);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-background-radius: 20;");    //testing    
        root.getChildren().addAll(bgView, content);
        root.setBackground(null);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT); // Essential for the "shape" of the image to show
        
        window.setWidth(400);
        window.setHeight(300);

        window.setScene(scene);
        window.centerOnScreen();
        window.showAndWait();
    }
}