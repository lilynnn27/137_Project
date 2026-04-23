package app.screens;

import app.Main;
import app.utils.UIUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class SinglePlayerScreen {
    private Scene scene;

    public SinglePlayerScreen(Main mainApp) {
        Label spTitle = new Label("Single Player Mode");
        spTitle.setStyle(UIUtils.TITLE_STYLE);
        
        Label spDesc = new Label("Milestone 1: Basic Game Logic\n(In Development)");
        spDesc.setStyle(UIUtils.SUBTITLE_STYLE);
        spDesc.setTextAlignment(TextAlignment.CENTER);

        Button btnBackSP = new Button("Back to Menu");
        UIUtils.styleButton(btnBackSP, UIUtils.BUTTON_STYLE, UIUtils.BUTTON_HOVER_STYLE);
        btnBackSP.setOnAction(e -> mainApp.showLandingPage());

        VBox spLayout = new VBox(40);
        spLayout.setAlignment(Pos.CENTER);
        spLayout.setStyle(UIUtils.BG_STYLE);
        spLayout.getChildren().addAll(spTitle, spDesc, btnBackSP);
        
        this.scene = new Scene(spLayout, 1024, 768);
    }

    public Scene getScene() {
        return scene;
    }
}
