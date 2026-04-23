package app.screens;

import app.Main;
import app.utils.UIUtils;
import javafx.geometry.Pos;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class LandingPageScreen {
    private VBox root;

    public LandingPageScreen(Main mainApp) {
        Label titleLabel = new Label("PROJECT 137");
        titleLabel.setStyle(UIUtils.TITLE_STYLE);
        
        Label subtitleLabel = new Label("Networked Game");
        subtitleLabel.setStyle(UIUtils.SUBTITLE_STYLE);

        Button btnSinglePlayer = new Button("Single Player");
        UIUtils.styleButton(btnSinglePlayer, UIUtils.BUTTON_STYLE, UIUtils.BUTTON_HOVER_STYLE);
        btnSinglePlayer.setOnAction(e -> mainApp.showSinglePlayer());

        Button btnMultiplayer = new Button("Multiplayer");
        UIUtils.styleButton(btnMultiplayer, UIUtils.BUTTON_STYLE, UIUtils.BUTTON_HOVER_STYLE);
        btnMultiplayer.setOnAction(e -> mainApp.showMultiplayer());

        Button btnExit = new Button("Exit Game");
        UIUtils.styleButton(btnExit, UIUtils.BUTTON_STYLE, UIUtils.BUTTON_HOVER_STYLE);
        btnExit.setOnAction(e -> mainApp.exitGame());

        VBox menuLayout = new VBox(25);
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.setStyle(UIUtils.BG_STYLE);
        menuLayout.getChildren().addAll(titleLabel, subtitleLabel, btnSinglePlayer, btnMultiplayer, btnExit);
        
        this.root = menuLayout;
    }

    public javafx.scene.Parent getRoot() {
        return root;
    }
}
