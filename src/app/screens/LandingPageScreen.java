package app.screens;

import app.Main;
import app.utils.UIUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LandingPageScreen {

    private final StackPane root;
    private Image bgImage;
    private javafx.scene.shape.Rectangle dimOverlay;

    public LandingPageScreen(Main mainApp) {

        bgImage = UIUtils.ImageCache.get("assets/images/MainBackground.jpg");

        root = new StackPane();
        root.setAlignment(Pos.CENTER);

        if (bgImage != null && !bgImage.isError()) {
            BackgroundImage bg = new BackgroundImage(
                    bgImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(100, 100, true, true, false, true));
            root.setBackground(new Background(bg));
        }

        // ── Title (StackPane with shadow + front) ──
        StackPane titleStack = new StackPane();
        titleStack.setAlignment(Pos.CENTER);

        Label titleShadow = new Label("DOUGHMINATION");
        titleShadow.setStyle("-fx-text-fill: #5D4037;");

        Label titleFront = new Label("DOUGHMINATION");
        titleFront.setStyle("-fx-text-fill: #b89664;");

        titleStack.getChildren().addAll(titleShadow, titleFront);

        // ── Clicking title shows modal ──
        titleStack.setOnMouseEntered(e -> titleFront.setStyle("-fx-text-fill: #ff9900;"));
        titleStack.setOnMouseExited(e -> titleFront.setStyle("-fx-text-fill: #b89664;"));
        titleStack.setOnMouseClicked(e -> showModeModal(mainApp));

        // ── Spacebar opens modal — root must be focusable ──
        root.setFocusTraversable(true);
        root.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE) showModeModal(mainApp);
        });

        // ── Subtitle ──
        Label subtitle = new Label("A NETWORKED GAME PROJECT");
        subtitle.setStyle("-fx-text-fill: #795548;");

        // ── Nav Buttons — all equal fixed width ──
        Button btnRules = makeNavBtn("How to Play");
        btnRules.setOnAction(e -> mainApp.showRules());

        Button btnDevs = makeNavBtn("Developers");
        btnDevs.setOnAction(e -> mainApp.showDevelopers());

        Button btnExit = makeNavBtn("Exit Game");
        btnExit.setOnAction(e -> mainApp.exitGame());

        // kept for modal use but not shown on main screen
        Button btnSingle = makeNavBtn("Single Player");
        btnSingle.setOnAction(e -> mainApp.showSinglePlayer());

        Button btnMulti = makeNavBtn("Multiplayer");
        btnMulti.setOnAction(e -> mainApp.showMultiplayer());

        HBox buttonRow = new HBox(btnRules, btnDevs, btnExit);
        buttonRow.setAlignment(Pos.CENTER);

        // Combine all content
        VBox content = new VBox(0, titleStack, subtitle, buttonRow);
        content.setAlignment(Pos.CENTER);

        root.getChildren().add(content);

        // ── Dim overlay (hidden by default, shown when modal is open) ──
        dimOverlay = new Rectangle();
        dimOverlay.setFill(Color.rgb(0, 0, 0, 0.55));
        dimOverlay.setVisible(false);
        dimOverlay.widthProperty().bind(root.widthProperty());
        dimOverlay.heightProperty().bind(root.heightProperty());
        root.getChildren().add(dimOverlay);

        root.widthProperty().addListener((obs, o, w) ->
                applyLayout(w.doubleValue(), root.getHeight(),
                        titleShadow, titleFront, subtitle,
                        btnRules, btnDevs, btnExit, btnSingle, btnMulti,
                        buttonRow, content));

        root.heightProperty().addListener((obs, o, h) ->
                applyLayout(root.getWidth(), h.doubleValue(),
                        titleShadow, titleFront, subtitle,
                        btnRules, btnDevs, btnExit, btnSingle, btnMulti,
                        buttonRow, content));

        javafx.application.Platform.runLater(() -> {
            applyLayout(root.getWidth(), root.getHeight(),
                    titleShadow, titleFront, subtitle,
                    btnRules, btnDevs, btnExit, btnSingle, btnMulti,
                    buttonRow, content);
            root.requestFocus();
        });
    }

    // ── Styled modal with brown title bar, dim overlay, and large icon mode buttons ──
    private void showModeModal(Main mainApp) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        // TRANSPARENT (not UNDECORATED) is required to avoid white corner edges
        dialog.initStyle(StageStyle.TRANSPARENT);

        // Show dim overlay on the main screen
        dimOverlay.setVisible(true);

        // Remove overlay when dialog closes for any reason
        dialog.setOnHidden(e -> dimOverlay.setVisible(false));

        // ── Title bar ──
        Label titleLabel = new Label("Choose Mode");
        titleLabel.setStyle(
            "-fx-text-fill: #fff3e0;" +
            "-fx-font-family: '" + UIUtils.MAIN_FONT + "';" +
            "-fx-font-size: 25px;"
        );

        Button btnClose = new Button("✕");
        btnClose.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #ffe0b2;" +
            "-fx-font-size: 25px;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 2 12 2 12;" +
            "-fx-border-width: 0;"
        );
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(
            "-fx-background-color: rgba(160,82,45,0.6);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 25px;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 2 12 2 12;" +
            "-fx-border-width: 0;" +
            "-fx-background-radius: 6;"
        ));
        btnClose.setOnMouseExited(e -> btnClose.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #ffe0b2;" +
            "-fx-font-size: 25px;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 2 12 2 12;" +
            "-fx-border-width: 0;"
        ));
        btnClose.setOnAction(e -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox titleBar = new HBox(titleLabel, spacer, btnClose);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(14, 10, 14, 22));
        titleBar.setStyle(
            "-fx-background-color: #795548;" +
            "-fx-background-radius: 18 18 0 0;"
        );

        // ── Soft subtitle inside modal body ──
        Label modalSubtitle = new Label("What kind of game would you like to play?");
        modalSubtitle.setStyle(
            "-fx-text-fill: #a1887f;" +
            "-fx-font-family: '" + UIUtils.MAIN_FONT + "';" +
            "-fx-font-size: 25px;"
        );
        modalSubtitle.setPadding(new Insets(18, 0, 4, 0));

        // ── Mode buttons ──
        Button btnPractice    = makeModeBtn("assets/images/single.png", "Single Player", 64);
        Button btnMultiplayer = makeModeBtn("assets/images/multi.png",  "Multiplayer", 96);

        btnPractice.setOnAction(e -> { dialog.close(); mainApp.showSinglePlayer(); });
        btnMultiplayer.setOnAction(e -> { dialog.close(); mainApp.showMultiplayer(); });

        HBox modeRow = new HBox(36, btnPractice, btnMultiplayer);
        modeRow.setAlignment(Pos.CENTER);
        modeRow.setPadding(new Insets(16, 48, 44, 48));

        VBox dialogBody = new VBox(0, titleBar, modalSubtitle, modeRow);
        dialogBody.setAlignment(Pos.CENTER);
        dialogBody.setStyle(
            "-fx-background-color: #fff8f0;" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: #bcaaa4;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 18;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 24, 0.1, 0, 6);"
        );

        // Wrap in a transparent StackPane so the drop-shadow isn't clipped
        StackPane wrapper = new StackPane(dialogBody);
        wrapper.setStyle("-fx-background-color: transparent;");
        wrapper.setPadding(new Insets(16));

        Scene scene = new Scene(wrapper);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.sizeToScene();

        // ── Centre dialog over the main window ──
        javafx.application.Platform.runLater(() -> {
            javafx.stage.Window owner = root.getScene().getWindow();
            dialog.setX(owner.getX() + (owner.getWidth()  - dialog.getWidth())  / 2);
            dialog.setY(owner.getY() + (owner.getHeight() - dialog.getHeight()) / 2);
        });

        dialog.showAndWait();
    }

    private static Button makeModeBtn(String imagePath, String label, double iconSize) {
        String normal =
            "-fx-background-color: #fff3e0;" +
            "-fx-border-color: #d7ccc8;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 14;" +
            "-fx-background-radius: 14;" +
            "-fx-text-fill: #6d4c41;" +
            "-fx-font-size: 18px;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 28 32 28 32;";

        String hovered =
            "-fx-background-color: #ff9900;" +
            "-fx-border-color: #ff9900;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 14;" +
            "-fx-background-radius: 14;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 18px;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 28 32 28 32;";

        Image img = UIUtils.ImageCache.get(imagePath);
        ImageView iconView = new ImageView(img);
        iconView.setFitWidth(iconSize);
        iconView.setFitHeight(iconSize);
        iconView.setPreserveRatio(true);

        Label textLabel = new Label(label);
        textLabel.setStyle(
            "-fx-font-family: '" + UIUtils.MAIN_FONT + "';" +
            "-fx-font-size: 16px;" +
            "-fx-text-fill: #6d4c41;"
        );

        VBox btnContent = new VBox(12, iconView, textLabel);
        btnContent.setAlignment(Pos.CENTER);

        Button b = new Button();
        b.setGraphic(btnContent);
        b.setPrefWidth(220);
        b.setPrefHeight(220);
        b.setStyle(normal);
        b.setOnMouseEntered(e -> {
            b.setStyle(hovered);
            textLabel.setStyle(
                "-fx-font-family: '" + UIUtils.MAIN_FONT + "';" +
                "-fx-font-size: 16px;" +
                "-fx-text-fill: white;"
            );
        });
        b.setOnMouseExited(e -> {
            b.setStyle(normal);
            textLabel.setStyle(
                "-fx-font-family: '" + UIUtils.MAIN_FONT + "';" +
                "-fx-font-size: 16px;" +
                "-fx-text-fill: #6d4c41;"
            );
        });
        return b;
    }

    // ── Layout logic ──
    private void applyLayout(double w, double h,
                             Label titleShadow, Label titleFront, Label subtitle,
                             Button btnRules, Button btnDevs, Button btnExit,
                             Button btnSingle, Button btnMulti,
                             HBox buttonRow, VBox content) {
        if (w <= 0 || h <= 0) return;

        double titleSz    = clamp(w * 0.072, 28, 108);
        double subtitleSz = clamp(w * 0.032, 14, 52);
        double btnSz      = clamp(w * 0.022, 11, 36);
        double btnGap     = clamp(w * 0.028, 12, 52);
        double vGap       = clamp(h * 0.016, 5, 20);

        titleShadow.setFont(Font.font(UIUtils.MAIN_FONT, titleSz));
        titleFront.setFont(Font.font(UIUtils.MAIN_FONT, titleSz));
        subtitle.setFont(Font.font(UIUtils.MAIN_FONT, subtitleSz));

        for (Button b : new Button[]{btnRules, btnDevs, btnExit, btnSingle, btnMulti}) {
            b.setFont(Font.font(UIUtils.MAIN_FONT, btnSz));

            double bw = clamp(w * 0.13, 175, 200);
            double bh = clamp(h * 0.06, 36, 52);

            b.setMinWidth(bw);
            b.setMaxWidth(Double.MAX_VALUE);

            b.setPrefHeight(bh);
            b.setMinHeight(bh);
            b.setMaxHeight(bh);
        }

        buttonRow.setSpacing(btnGap);
        content.setSpacing(vGap);

        double sh = clamp(titleSz * 0.04, 2, 5);
        titleShadow.setTranslateX(sh);
        titleShadow.setTranslateY(sh);

        content.setTranslateY(h * 0.075);
    }

    private static Button makeNavBtn(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-btn");
        b.setMnemonicParsing(false);
        b.setMinHeight(Region.USE_PREF_SIZE);
        return b;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public javafx.scene.Parent getRoot() {
        return root;
    }
}