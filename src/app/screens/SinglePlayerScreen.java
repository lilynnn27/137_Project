package app.screens;

import app.Main;
import app.utils.UIUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import java.io.File;

public class SinglePlayerScreen {

    private static final String CREAM  = "#f5e6c8";
    private static final String BROWN  = "#3d282e";
    private static final String ORANGE = "#c46a2d";
    private static final String GOLD   = "#b89664";
    private static final String MUTED  = "#888888";

    private static final String BTN_PRI_N =
        "-fx-background-color:" + ORANGE + ";-fx-text-fill:" + CREAM + ";" +
        "-fx-border-color:" + ORANGE + ";-fx-border-width:2px;" +
        "-fx-padding:12 36;-fx-cursor:hand;";
    private static final String BTN_PRI_H =
        "-fx-background-color:" + CREAM + ";-fx-text-fill:" + BROWN + ";" +
        "-fx-border-color:" + ORANGE + ";-fx-border-width:2px;" +
        "-fx-padding:12 36;-fx-cursor:hand;";
    private static final String BTN_SEC_N =
        "-fx-background-color:transparent;-fx-text-fill:" + CREAM + ";" +
        "-fx-border-color:" + CREAM + ";-fx-border-width:2px;" +
        "-fx-padding:12 36;-fx-cursor:hand;";
    private static final String BTN_SEC_H =
        "-fx-background-color:" + CREAM + ";-fx-text-fill:" + BROWN + ";" +
        "-fx-border-color:" + CREAM + ";-fx-border-width:2px;" +
        "-fx-padding:12 36;-fx-cursor:hand;";

    private final StackPane root;

    public SinglePlayerScreen(Main mainApp) {
        root = new StackPane();

        File bgFile = new File("assets/images/MainBackground.jpg");
        if (bgFile.exists()) {
            ImageView bg = new ImageView(new Image(bgFile.toURI().toString()));
            bg.setPreserveRatio(false);
            bg.fitWidthProperty().bind(root.widthProperty());
            bg.fitHeightProperty().bind(root.heightProperty());
            root.getChildren().add(bg);
        }

        Rectangle overlay = new Rectangle();
        overlay.widthProperty().bind(root.widthProperty());
        overlay.heightProperty().bind(root.heightProperty());
        overlay.setFill(Color.web("#000000", 0.62));
        root.getChildren().add(overlay);

        Label title = new Label("Single Player Mode");
        title.setFont(Font.font(UIUtils.MAIN_FONT, 38));
        title.setStyle("-fx-text-fill:" + GOLD + ";");

        Label subtitle = new Label("Milestone 1: Basic Game Logic  •  In Development");
        subtitle.setFont(Font.font(UIUtils.MAIN_FONT, 18));
        subtitle.setStyle("-fx-text-fill:" + MUTED + ";");
        subtitle.setTextAlignment(TextAlignment.CENTER);

        VBox titleBox = new VBox(6, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);

        Label trayHeader = new Label("The Tray");
        trayHeader.setFont(Font.font(UIUtils.MAIN_FONT, 26));
        trayHeader.setStyle("-fx-text-fill:" + GOLD + ";");

        String[] colors = {"orange", "blue", "green", "red"};
        String[] names  = {"Player 1", "Player 2", "Player 3", "Player 4"};
        HBox slotsRow = new HBox(20);
        slotsRow.setAlignment(Pos.CENTER);
        slotsRow.getChildren().add(buildFilledSlot(colors[0], names[0]));
        for (int i = 1; i < 4; i++)
            slotsRow.getChildren().add(buildEmptySlot(colors[i], names[i]));

        VBox trayBox = new VBox(14, trayHeader, slotsRow);
        trayBox.setAlignment(Pos.CENTER);

        Button btnPlay = new Button("Play Now");
        btnPlay.setFont(Font.font(UIUtils.MAIN_FONT, 20));
        btnPlay.setStyle(BTN_PRI_N);
        btnPlay.setOnMouseEntered(e -> btnPlay.setStyle(BTN_PRI_H));
        btnPlay.setOnMouseExited (e -> btnPlay.setStyle(BTN_PRI_N));
        btnPlay.setOnAction(e -> mainApp.showGamePlay());

        Button btnBack = new Button("Back to Menu");
        btnBack.setFont(Font.font(UIUtils.MAIN_FONT, 20));
        btnBack.setStyle(BTN_SEC_N);
        btnBack.setOnMouseEntered(e -> btnBack.setStyle(BTN_SEC_H));
        btnBack.setOnMouseExited (e -> btnBack.setStyle(BTN_SEC_N));
        btnBack.setOnAction(e -> mainApp.showLandingPage());

        HBox buttonRow = new HBox(30, btnPlay, btnBack);
        buttonRow.setAlignment(Pos.CENTER);

        VBox content = new VBox(32, titleBox, trayBox, buttonRow, buildPreviewStrip());
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(50));
        root.getChildren().add(content);
    }

    private StackPane buildFilledSlot(String color, String playerName) {
        StackPane slot = new StackPane();
        slot.setPrefSize(130, 160);
        slot.setStyle(
            "-fx-background-color:rgba(61,40,46,0.75);-fx-background-radius:10;" +
            "-fx-border-color:#b89664;-fx-border-width:2;-fx-border-radius:10;"
        );
        File imgFile = new File("assets/images/PlayersDough/" + color + ".png");
        ImageView icon = new ImageView();
        if (imgFile.exists()) icon.setImage(new Image(imgFile.toURI().toString()));
        icon.setFitWidth(64); icon.setFitHeight(64); icon.setPreserveRatio(true);
        Label name = new Label(playerName);
        name.setFont(Font.font(UIUtils.MAIN_FONT, 15));
        name.setStyle("-fx-text-fill:#f5e6c8;");
        VBox inner = new VBox(8, icon, name);
        inner.setAlignment(Pos.CENTER);
        slot.getChildren().add(inner);
        return slot;
    }

    private StackPane buildEmptySlot(String color, String playerName) {
        StackPane slot = new StackPane();
        slot.setPrefSize(130, 160);
        slot.setStyle(
            "-fx-background-color:rgba(20,15,18,0.55);-fx-background-radius:10;" +
            "-fx-border-color:#555555;-fx-border-width:2;-fx-border-radius:10;" +
            "-fx-border-style:dashed;"
        );
        File imgFile = new File("assets/images/PlayersDough/" + color + ".png");
        ImageView icon = new ImageView();
        if (imgFile.exists()) icon.setImage(new Image(imgFile.toURI().toString()));
        icon.setFitWidth(64); icon.setFitHeight(64); icon.setPreserveRatio(true);
        icon.setOpacity(0.3);
        Label waiting = new Label("Waiting...");
        waiting.setFont(Font.font(UIUtils.MAIN_FONT, 14));
        waiting.setStyle("-fx-text-fill:#888888;");
        VBox inner = new VBox(8, icon, waiting);
        inner.setAlignment(Pos.CENTER);
        slot.getChildren().add(inner);
        return slot;
    }

    private VBox buildPreviewStrip() {
        Label header = new Label("What's in the kitchen:");
        header.setFont(Font.font(UIUtils.MAIN_FONT, 16));
        header.setStyle("-fx-text-fill:#888888;");
        String[] paths = {
            "assets/images/hazard/RollingPin-Hazard.png",
            "assets/images/hazard/Ice-Hazard.png",
            "assets/images/hazard/RottenEgg-Hazard.png",
            "assets/images/powerup/Oil-Powerup.png",
            "assets/images/powerup/Dough-Powerup.png",
            "assets/images/powerup/Flour-Powerup.png"
        };
        HBox iconRow = new HBox(18);
        iconRow.setAlignment(Pos.CENTER);
        for (String path : paths) {
            File f = new File(path);
            if (f.exists()) {
                ImageView iv = new ImageView(new Image(f.toURI().toString()));
                iv.setFitWidth(32); iv.setFitHeight(32); iv.setPreserveRatio(true);
                iconRow.getChildren().add(iv);
            }
        }
        VBox strip = new VBox(8, header, iconRow);
        strip.setAlignment(Pos.CENTER);
        return strip;
    }

    public javafx.scene.Parent getRoot() { return root; }
}
