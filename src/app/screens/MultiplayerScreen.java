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
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import java.io.File;

public class MultiplayerScreen {

    private final VBox root;

    private static final String[] PLAYER_SPRITES = { "orange", "blue", "green", "red" };
    private static final String[] PLAYER_NAMES   = { "Player 1", "Waiting...", "Waiting...", "Waiting..." };
    private static final String[] PLAYER_COLORS  = { "#FF8C00", "#1565C0", "#2E7D32", "#C62828" };

    public MultiplayerScreen(Main mainApp) {
        // --- Title ---
        Label title = new Label("The Tray");
        title.setFont(Font.font(UIUtils.MAIN_FONT, 72));
        title.setStyle("-fx-text-fill: #b89664;");

        Label subtitle = new Label("Lobby");
        subtitle.setFont(Font.font(UIUtils.MAIN_FONT, 28));
        subtitle.setStyle("-fx-text-fill: #AAAAAA;");

        VBox titleBox = new VBox(4, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);

        // --- Player list ---
        VBox playerList = new VBox(8);
        playerList.setAlignment(Pos.CENTER_LEFT);
        playerList.setPadding(new Insets(20));
        playerList.setStyle("-fx-background-color: #111111; -fx-background-radius: 12;");

        Label listHeader = new Label("Players");
        listHeader.setFont(Font.font(UIUtils.MAIN_FONT, 26));
        listHeader.setStyle("-fx-text-fill: white;");
        VBox.setMargin(listHeader, new Insets(0, 0, 8, 0));
        playerList.getChildren().add(listHeader);

        for (int i = 0; i < 4; i++) {
            playerList.getChildren().add(buildPlayerSlot(i));
        }

        // --- Arena preview ---
        StackPane arenaPreview = buildArenaPreview();

        // --- Center section ---
        HBox center = new HBox(40, playerList, arenaPreview);
        center.setAlignment(Pos.CENTER);

        // --- Status label ---
        Label statusLabel = new Label("Waiting for players…");
        statusLabel.setFont(Font.font(UIUtils.MAIN_FONT, 30));
        statusLabel.setStyle("-fx-text-fill: #AAAAAA;");

        // --- Ready button ---
        final boolean[] isReady = { false };
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: white; "
                           + "-fx-border-color: white; -fx-border-width: 2px; "
                           + "-fx-padding: 15 40; -fx-cursor: hand;";
        String readyStyle  = "-fx-background-color: #4CAF50; -fx-text-fill: white; "
                           + "-fx-border-color: #4CAF50; -fx-border-width: 2px; "
                           + "-fx-padding: 15 40; -fx-cursor: hand;";

        Button readyBtn = new Button("Ready");
        readyBtn.setFont(Font.font(UIUtils.MAIN_FONT, 26));
        readyBtn.setStyle(normalStyle);
        readyBtn.setOnMouseEntered(e -> { if (!isReady[0]) readyBtn.setStyle(UIUtils.BUTTON_HOVER_STYLE); });
        readyBtn.setOnMouseExited(e  -> { if (!isReady[0]) readyBtn.setStyle(normalStyle); });
        readyBtn.setOnAction(e -> {
            isReady[0] = !isReady[0];
            readyBtn.setStyle(isReady[0] ? readyStyle : normalStyle);
            readyBtn.setText(isReady[0] ? "Ready!" : "Ready");
            statusLabel.setText(isReady[0] ? "Ready to Fry!" : "Waiting for players…");
        });

        // --- Back button ---
        Button backBtn = new Button("Back to Menu");
        UIUtils.styleButton(backBtn, UIUtils.BUTTON_STYLE, UIUtils.BUTTON_HOVER_STYLE);
        backBtn.setOnAction(e -> mainApp.showLandingPage());

        HBox buttons = new HBox(30, readyBtn, backBtn);
        buttons.setAlignment(Pos.CENTER);

        // --- Root ---
        VBox layout = new VBox(28, titleBox, center, statusLabel, buttons);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle(UIUtils.BG_STYLE);
        layout.setPadding(new Insets(40));

        this.root = layout;
    }

    private HBox buildPlayerSlot(int index) {
        HBox slot = new HBox(14);
        slot.setAlignment(Pos.CENTER_LEFT);
        slot.setPadding(new Insets(10, 18, 10, 18));
        slot.setPrefWidth(300);
        slot.setStyle("-fx-background-color: " + (index % 2 == 0 ? "#1c1c1c" : "#141414")
                    + "; -fx-background-radius: 8;");

        boolean filled = index == 0;

        if (filled) {
            File spriteFile = new File("assets/images/PlayersDough/" + PLAYER_SPRITES[index] + ".png");
            ImageView icon = new ImageView();
            if (spriteFile.exists()) {
                icon.setImage(new Image(spriteFile.toURI().toString()));
            }
            icon.setFitWidth(44);
            icon.setFitHeight(44);
            icon.setPreserveRatio(true);

            Label name = new Label(PLAYER_NAMES[index]);
            name.setFont(Font.font(UIUtils.MAIN_FONT, 22));
            name.setStyle("-fx-text-fill: " + PLAYER_COLORS[index] + ";");

            slot.getChildren().addAll(icon, name);
        } else {
            Circle placeholder = new Circle(22);
            placeholder.setFill(Color.web("#2a2a2a"));
            placeholder.setStroke(Color.web("#444444"));
            placeholder.setStrokeWidth(2);

            Label name = new Label(PLAYER_NAMES[index]);
            name.setFont(Font.font(UIUtils.MAIN_FONT, 22));
            name.setStyle("-fx-text-fill: #444444;");

            slot.getChildren().addAll(placeholder, name);
        }

        return slot;
    }

    private StackPane buildArenaPreview() {
        StackPane preview = new StackPane();
        preview.setPrefSize(300, 300);
        preview.setMaxSize(300, 300);

        File bgFile = new File("assets/images/GameplayBackground.jpg");
        if (bgFile.exists()) {
            ImageView bg = new ImageView(new Image(bgFile.toURI().toString()));
            bg.setFitWidth(300);
            bg.setFitHeight(300);
            bg.setPreserveRatio(false);
            bg.setClip(new Circle(150, 150, 145));
            preview.getChildren().add(bg);
        }

        Circle border = new Circle(145);
        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.web("#b89664"));
        border.setStrokeWidth(3);
        preview.getChildren().add(border);

        Label previewLabel = new Label("Arena Preview");
        previewLabel.setFont(Font.font(UIUtils.MAIN_FONT, 16));
        previewLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5);");
        StackPane.setAlignment(previewLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(previewLabel, new Insets(0, 0, 14, 0));
        preview.getChildren().add(previewLabel);

        return preview;
    }

    public javafx.scene.Parent getRoot() {
        return root;
    }
}
