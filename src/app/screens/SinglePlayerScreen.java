package app.screens;

import app.Main;
import app.utils.UIUtils;
import javafx.geometry.Pos;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class SinglePlayerScreen {
    private VBox root;

    public SinglePlayerScreen(Main mainApp) {
        Label spTitle = new Label("Single Player Mode");
        spTitle.setStyle(UIUtils.TITLE_STYLE);
        
        Label spDesc = new Label("Milestone 1: Basic Game Logic\n(In Development)");
        spDesc.setStyle(UIUtils.SUBTITLE_STYLE);
        spDesc.setTextAlignment(TextAlignment.CENTER);

        Button btnPlay = new Button("Play Now");
        UIUtils.styleButton(btnPlay, UIUtils.BUTTON_STYLE, UIUtils.BUTTON_HOVER_STYLE);
        btnPlay.setOnAction(e -> mainApp.showGamePlay());

        Button btnBackSP = new Button("Back to Menu");
        UIUtils.styleButton(btnBackSP, UIUtils.BUTTON_STYLE, UIUtils.BUTTON_HOVER_STYLE);
        btnBackSP.setOnAction(e -> mainApp.showLandingPage());

        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(30, btnPlay, btnBackSP);
        buttonBox.setAlignment(Pos.CENTER);

        VBox spLayout = new VBox(40);
        spLayout.setAlignment(Pos.CENTER);
        spLayout.setStyle(UIUtils.BG_STYLE);
        spLayout.getChildren().addAll(spTitle, spDesc, buttonBox);
        
        this.root = spLayout;
    }

    public javafx.scene.Parent getRoot() {
        return root;
    }
}
