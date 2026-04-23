package app.screens;

import app.Main;
import app.utils.UIUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class MultiplayerScreen {
    private Scene scene;

    public MultiplayerScreen(Main mainApp) {
        Label mpTitle = new Label("Multiplayer Mode");
        mpTitle.setStyle(UIUtils.TITLE_STYLE);
        
        Label mpDesc = new Label("Milestone 2: Networked Game\nWaiting for 4 players to connect...");
        mpDesc.setStyle(UIUtils.SUBTITLE_STYLE);
        mpDesc.setTextAlignment(TextAlignment.CENTER);

        Button btnBackMP = new Button("Back to Menu");
        UIUtils.styleButton(btnBackMP, UIUtils.BUTTON_STYLE, UIUtils.BUTTON_HOVER_STYLE);
        btnBackMP.setOnAction(e -> mainApp.showLandingPage());

        VBox mpLayout = new VBox(40);
        mpLayout.setAlignment(Pos.CENTER);
        mpLayout.setStyle(UIUtils.BG_STYLE);
        mpLayout.getChildren().addAll(mpTitle, mpDesc, btnBackMP);
        
        this.scene = new Scene(mpLayout, 1024, 768);
    }

    public Scene getScene() {
        return scene;
    }
}
