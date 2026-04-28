package app.screens;

import java.io.File;

import app.Main;
import app.utils.UIUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
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
        mainApp.setBackgroundBlur(true); // Apply blur to the main game screen
        
        File file = new File("assets/images/GameOverModal.png");
        ImageView bgView = new ImageView();
        if (file.exists()) {
            bgView.setImage(new Image(file.toURI().toString()));
        }

        bgView.setPreserveRatio(false);
        bgView.fitWidthProperty().bind(window.widthProperty());
        bgView.fitHeightProperty().bind(window.heightProperty());

        StackPane titleStack = new StackPane();
        titleStack.setAlignment(Pos.CENTER);

        //Shadow part ng title
        Label titleBottom = new Label("GAME OVER");
        titleBottom.setFont(Font.font(UIUtils.MAIN_FONT, 100));
        titleBottom.setStyle("-fx-text-fill: #ba2b00;"); // RED
        // Offset this layer slightly to the right and down for shadow effect
        titleBottom.setTranslateX(4); 
        titleBottom.setTranslateY(4);

        //Lighter part ng title
        Label titleTop = new Label("GAME OVER");
        titleTop.setFont(Font.font(UIUtils.MAIN_FONT, 100));
        titleTop.setStyle("-fx-text-fill: #b89664;"); // Light Cream/White

        titleStack.getChildren().addAll(titleBottom, titleTop);
        VBox.setMargin(titleStack, new Insets(20, 0, 0, 0));

        double percent = ((double) score / total) * 100;
        Label scoreLabel = new Label(String.format("Final Territory: %.1f%%", percent));
        scoreLabel.setFont(Font.font(UIUtils.MAIN_FONT, 50));
        scoreLabel.setStyle(" -fx-text-fill: #795548;");

        // BUTTON CONTAINER
        HBox buttonContainer = new HBox(40);
        buttonContainer.setAlignment(Pos.CENTER);

        // TEXT-ONLY BUTTON STYLES (Fixed semicolon error)
        String textButtonStyle = "-fx-background-color: transparent; " +
                                "-fx-text-fill: #5D4037; " + // Fixed: added semicolon here
                                "-fx-cursor: hand;";

        String textButtonHoverStyle = "-fx-background-color: transparent; " +
                                     "-fx-text-fill: #ff9900; " + 
                                     "-fx-cursor: hand;"; 

        // SINGLE PLAYER
        Button retryBtn = new Button("Retry");
        retryBtn.setFont(Font.font(UIUtils.MAIN_FONT, 45)); 
        retryBtn.setStyle(textButtonStyle);
        retryBtn.setOnMouseEntered(e -> retryBtn.setStyle(textButtonHoverStyle));
        retryBtn.setOnMouseExited(e -> retryBtn.setStyle(textButtonStyle));
        retryBtn.setOnAction(e -> {
            window.close();
            mainApp.showGamePlay(); // This triggers the fresh start in Main.java
        });

        Button menuBtn = new Button("BACK TO MENU");
        menuBtn.setFont(Font.font(UIUtils.MAIN_FONT, 45));  
        menuBtn.setStyle(textButtonStyle);
        menuBtn.setOnMouseEntered(e -> menuBtn.setStyle(textButtonHoverStyle)); 
        menuBtn.setOnMouseExited(e -> menuBtn.setStyle(textButtonStyle));
        menuBtn.setOnAction(e -> {
            window.close();
            mainApp.showLandingPage();
        });
        
        buttonContainer.getChildren().addAll(retryBtn, menuBtn);

        VBox content = new VBox(20, titleStack, scoreLabel, buttonContainer);
        content.setAlignment(Pos.CENTER);

        StackPane root = new StackPane();
        // root.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-background-radius: 20;");    //testing    
        root.getChildren().addAll(bgView, content);
        root.setBackground(null);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT); // Essential for the "shape" of the image to show
        
        window.setWidth(1600);
        window.setHeight(800);
        window.setScene(scene);
        window.centerOnScreen();
        window.showAndWait();
        mainApp.setBackgroundBlur(false); // Remove blur when modal is closed   
    }
}