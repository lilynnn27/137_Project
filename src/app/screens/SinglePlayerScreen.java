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
public class SinglePlayerScreen {

    private static final String CREAM  = "#f5e6c8";
    private static final String BROWN  = "#3d282e";
    private static final String ORANGE = "#c46a2d";
    private static final String GOLD   = "#b89664";
    private static final String MUTED  = "#888888";

    // Font constants — loaded once, applied at construction, never re-set
    private static final Font FONT_TITLE  = Font.font(UIUtils.MAIN_FONT, 38);
    private static final Font FONT_HEADER = Font.font(UIUtils.MAIN_FONT, 26);
    private static final Font FONT_BUTTON = Font.font(UIUtils.MAIN_FONT, 20);
    private static final Font FONT_SMALL  = Font.font(UIUtils.MAIN_FONT, 16);

    private static final String BTN_PRI_N =
        "-fx-background-color:" + ORANGE + ";-fx-text-fill:" + CREAM + ";" +
        "-fx-border-color:" + ORANGE + ";-fx-border-width:2px;" +
        "-fx-padding:12 36;-fx-cursor:hand;" +
        "-fx-font-family:'" + UIUtils.MAIN_FONT + "';-fx-font-size:20px;-fx-font-weight:bold;";
    private static final String BTN_PRI_H =
        "-fx-background-color:" + CREAM + ";-fx-text-fill:" + BROWN + ";" +
        "-fx-border-color:" + ORANGE + ";-fx-border-width:2px;" +
        "-fx-padding:12 36;-fx-cursor:hand;" +
        "-fx-font-family:'" + UIUtils.MAIN_FONT + "';-fx-font-size:20px;-fx-font-weight:bold;";
    private static final String BTN_SEC_N =
        "-fx-background-color:transparent;-fx-text-fill:" + CREAM + ";" +
        "-fx-border-color:" + CREAM + ";-fx-border-width:2px;" +
        "-fx-padding:12 36;-fx-cursor:hand;" +
        "-fx-font-family:'" + UIUtils.MAIN_FONT + "';-fx-font-size:20px;-fx-font-weight:bold;";
    private static final String BTN_SEC_H =
        "-fx-background-color:" + CREAM + ";-fx-text-fill:" + BROWN + ";" +
        "-fx-border-color:" + CREAM + ";-fx-border-width:2px;" +
        "-fx-padding:12 36;-fx-cursor:hand;" +
        "-fx-font-family:'" + UIUtils.MAIN_FONT + "';-fx-font-size:20px;-fx-font-weight:bold;";

    private final StackPane root;

    // Image fields — held here so they are never GC'd while the screen is displayed
    private Image bgImage;
    private static final String[] DOUGH_FILES = {
            "orange", "blue", "green", "red", "yellow", "pink", "purple", "indigo"
    };
    private final Image[] doughImages   = new Image[DOUGH_FILES.length];
    private int currentDoughIndex = 0;
    private final Image[] previewImages = new Image[6]; // 3 hazards + 3 powerups

    public SinglePlayerScreen(Main mainApp) {
        root = new StackPane();

        bgImage = UIUtils.ImageCache.get("assets/images/MainBackground.jpg");

        for (int i = 0; i < DOUGH_FILES.length; i++) {
            doughImages[i] = UIUtils.ImageCache.get("assets/images/PlayersDough/" + DOUGH_FILES[i] + ".png");
        }

        String[] pvPaths = {
            "assets/images/hazard/RollingPin-Hazard.png",
            "assets/images/hazard/Ice-Hazard.png",
            "assets/images/hazard/RottenEgg-Hazard.png",
            "assets/images/powerup/Oil-Powerup.png",
            "assets/images/powerup/Dough-Powerup.png",
            "assets/images/powerup/Flour-Powerup.png"
        };
        for (int i = 0; i < pvPaths.length; i++) {
            previewImages[i] = UIUtils.ImageCache.get(pvPaths[i]);
        }

        // Background
        if (bgImage != null && !bgImage.isError()) {
            ImageView bg = new ImageView(bgImage);
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

        // Title
        Label title = new Label("Single Player Mode");
        title.setFont(FONT_TITLE);
        title.setStyle("-fx-text-fill:" + GOLD + ";");

        VBox titleBox = new VBox(6, title);
        titleBox.setAlignment(Pos.CENTER);

        // The Tray
        Label trayHeader = new Label("The Tray");
        trayHeader.setFont(FONT_HEADER);
        trayHeader.setStyle("-fx-text-fill:" + GOLD + ";");

        HBox slotRow = new HBox();
        slotRow.setAlignment(Pos.CENTER);
        slotRow.getChildren().add(buildFilledSlot("Player 1"));

        VBox trayBox = new VBox(14, trayHeader, slotRow);
        trayBox.setAlignment(Pos.CENTER);

        // Buttons
        Button btnPlay = new Button("Play Now");
        btnPlay.setFont(FONT_BUTTON);
        btnPlay.setStyle(BTN_PRI_N);
        btnPlay.setOnMouseEntered(e -> btnPlay.setStyle(BTN_PRI_H));
        btnPlay.setOnMouseExited (e -> btnPlay.setStyle(BTN_PRI_N));
        btnPlay.setOnAction(e -> mainApp.showGamePlay(DOUGH_FILES[currentDoughIndex]));

        Button btnBack = new Button("Back to Menu");
        btnBack.setFont(FONT_BUTTON);
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

    private StackPane buildFilledSlot(String playerName) {
        StackPane slot = new StackPane();
        slot.setPrefSize(260, 260);
        slot.setStyle(
            "-fx-background-color:rgba(61,40,46,0.75);-fx-background-radius:16;" +
            "-fx-border-color:#b89664;-fx-border-width:4;-fx-border-radius:16;"
        );
        ImageView icon = new ImageView();
        icon.setImage(doughImages[currentDoughIndex]);
        icon.setFitWidth(140); icon.setFitHeight(140); icon.setPreserveRatio(true);

        Button leftBtn = new Button("<");
        leftBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #b89664; -fx-font-size: 28px; -fx-font-weight: bold; -fx-cursor: hand;");
        leftBtn.setOnAction(e -> {
            currentDoughIndex = (currentDoughIndex - 1 + DOUGH_FILES.length) % DOUGH_FILES.length;
            icon.setImage(doughImages[currentDoughIndex]);
        });

        Button rightBtn = new Button(">");
        rightBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #b89664; -fx-font-size: 28px; -fx-font-weight: bold; -fx-cursor: hand;");
        rightBtn.setOnAction(e -> {
            currentDoughIndex = (currentDoughIndex + 1) % DOUGH_FILES.length;
            icon.setImage(doughImages[currentDoughIndex]);
        });

        HBox imageRow = new HBox(5, leftBtn, icon, rightBtn);
        imageRow.setAlignment(Pos.CENTER);

        Label name = new Label(playerName);
        name.setFont(FONT_HEADER);
        name.setStyle("-fx-text-fill:#f5e6c8;");
        VBox inner = new VBox(20, imageRow, name);
        inner.setAlignment(Pos.CENTER);
        slot.getChildren().add(inner);
        return slot;
    }


    private VBox buildPreviewStrip() {
        Label header = new Label("What's in the kitchen:");
        header.setFont(FONT_SMALL);
        header.setStyle("-fx-text-fill:#888888;");
        HBox iconRow = new HBox(18);
        iconRow.setAlignment(Pos.CENTER);
        for (Image img : previewImages) {
            if (img != null && !img.isError()) {
                ImageView iv = new ImageView(img);
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
