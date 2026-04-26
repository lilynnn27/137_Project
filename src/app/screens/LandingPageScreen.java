package app.screens;

import app.Main;
import app.utils.UIUtils;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import java.io.File;

public class LandingPageScreen {
    private VBox root;

    public LandingPageScreen(Main mainApp) {
        this.root = new VBox(25);
        this.root.setAlignment(Pos.TOP_CENTER); 
        
        this.root.setPadding(new Insets(400, 0, 0, 0)); 

        File bgFile = new File("assets/images/MainBackground.jpg");
        if (bgFile.exists()) {
            Image bgImage = new Image(bgFile.toURI().toString());
            BackgroundImage background = new BackgroundImage(
                    bgImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(100, 100, true, true, false, true));
            this.root.setBackground(new Background(background));
        }

        //stack pane para mapagstack yung 2 titles
        StackPane titleStack = new StackPane();
        titleStack.setAlignment(Pos.CENTER);

        //Shadow part ng title
        Label titleBottom = new Label("EMPANADA DOUGHMINATION");
        titleBottom.setFont(Font.font(UIUtils.MAIN_FONT, 110));
        titleBottom.setStyle("-fx-text-fill: #5D4037;"); // Dark Brown
        // Offset this layer slightly to the right and down for shadow effect
        titleBottom.setTranslateX(4); 
        titleBottom.setTranslateY(4);

        //Lighter part ng title
        Label titleTop = new Label("EMPANADA DOUGHMINATION");
        titleTop.setFont(Font.font(UIUtils.MAIN_FONT, 110));
        titleTop.setStyle("-fx-text-fill: #b89664;"); // Light Cream/White

        //Add to stackkk
        titleStack.getChildren().addAll(titleBottom, titleTop);
        VBox.setMargin(titleStack, new Insets(20, 0, 0, 0));


        // SUBTITLE
        String subtitleColor = "-fx-text-fill: #795548;";
        Label subtitleLabel = new Label("A NETWORKED GAME PROJECT");
        subtitleLabel.setStyle(UIUtils.SUBTITLE_STYLE + subtitleColor);
        subtitleLabel.setFont(Font.font(UIUtils.MAIN_FONT, 50));
        VBox.setMargin(subtitleLabel, new Insets(-35, 0, 0, 0));

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
        Button btnSinglePlayer = new Button("Single Player");
        btnSinglePlayer.setFont(Font.font(UIUtils.MAIN_FONT, 45)); 
        btnSinglePlayer.setStyle(textButtonStyle);
        btnSinglePlayer.setOnMouseEntered(e -> btnSinglePlayer.setStyle(textButtonHoverStyle));
        btnSinglePlayer.setOnMouseExited(e -> btnSinglePlayer.setStyle(textButtonStyle));
        btnSinglePlayer.setOnAction(e -> mainApp.showSinglePlayer());

        // MULTIPLAYER
        Button btnMultiplayer = new Button("Multiplayer");
        btnMultiplayer.setFont(Font.font(UIUtils.MAIN_FONT, 45)); 
        btnMultiplayer.setStyle(textButtonStyle);
        btnMultiplayer.setOnMouseEntered(e -> btnMultiplayer.setStyle(textButtonHoverStyle));
        btnMultiplayer.setOnMouseExited(e -> btnMultiplayer.setStyle(textButtonStyle));
        btnMultiplayer.setOnAction(e -> mainApp.showMultiplayer());

        // EXIT GAME
        Button btnExit = new Button("Exit Game");
        btnExit.setFont(Font.font(UIUtils.MAIN_FONT, 45)); 
        btnExit.setStyle(textButtonStyle);
        btnExit.setOnMouseEntered(e -> btnExit.setStyle(textButtonHoverStyle));
        btnExit.setOnMouseExited(e -> btnExit.setStyle(textButtonStyle));
        btnExit.setOnAction(e -> mainApp.exitGame());

        buttonContainer.getChildren().addAll(btnSinglePlayer, btnMultiplayer, btnExit);

        this.root.getChildren().addAll(titleStack, subtitleLabel, buttonContainer);
    }

    public javafx.scene.Parent getRoot() {
        return root;
    }
}