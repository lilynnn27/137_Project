package app.screens;

import app.Main;
import app.utils.UIUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

public class DevelopersPage {
    private final StackPane root;

    private static final String ORANGE = "#ff9900";
    private static final String BROWN = "#3d282e";

    private static final String BTN_SEC_NORMAL =
            "-fx-background-color: transparent; -fx-text-fill: " + BROWN + "; " +
            "-fx-border-color: " + BROWN + "; -fx-border-width: 2px; " +
            "-fx-padding: 12 30; -fx-cursor: hand; -fx-font-weight: bold; " +
            "-fx-font-family: '" + UIUtils.MAIN_FONT + "'; -fx-font-size: 20px;";

    private static final String BTN_SEC_HOVER =
            "-fx-background-color: " + BROWN + "; -fx-text-fill: " + ORANGE + "; " +
            "-fx-border-color: " + BROWN + "; -fx-border-width: 2px; " +
            "-fx-padding: 12 30; -fx-cursor: hand; -fx-font-weight: bold; " +
            "-fx-font-family: '" + UIUtils.MAIN_FONT + "'; -fx-font-size: 20px;";

    // { imagePath, fullName }
    private static final String[][] DEVELOPERS = {
        { "assets/images/dev1.png", "Erin Reilley A. Amistoso" },
        { "assets/images/dev2.png", "Mary Eunice S. Magnaye" },
        { "assets/images/dev3.png", "Edgar Alan Emmanuel B. Tiamzon" }
    };

    public DevelopersPage(Main mainApp) {

        Image bgImage = UIUtils.ImageCache.get("assets/images/MainBackground.jpg");

        root = new StackPane();
        root.setAlignment(Pos.CENTER);

        // ── Background ──
        if (bgImage != null && !bgImage.isError()) {
            BackgroundImage bg = new BackgroundImage(
                    bgImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(100, 100, true, true, false, true));
            root.setBackground(new Background(bg));
        }

        // ── Title (shadow + front layer) ──
        StackPane titleStack = new StackPane();
        titleStack.setAlignment(Pos.CENTER);

        Label titleShadow = new Label("DEVELOPERS");
        titleShadow.setStyle("-fx-text-fill: #5D4037;");
        titleShadow.setFont(Font.font(UIUtils.MAIN_FONT, 72));
        titleShadow.setTranslateX(3);
        titleShadow.setTranslateY(3);

        Label titleFront = new Label("DEVELOPERS");
        titleFront.setStyle("-fx-text-fill: #b89664;");
        titleFront.setFont(Font.font(UIUtils.MAIN_FONT, 72));

        titleStack.getChildren().addAll(titleShadow, titleFront);

        // ── Developer cards ──
        VBox cardsBox = new VBox(0); // no gap; each card fills its row evenly
        cardsBox.setAlignment(Pos.CENTER);
        cardsBox.setPadding(new Insets(2, 8, 2, 8));

        for (int i = 0; i < DEVELOPERS.length; i++) {
            HBox card = buildDeveloperCard(DEVELOPERS[i][0], DEVELOPERS[i][1], i % 2 != 0);
            cardsBox.getChildren().add(card);
        }

        // ── Styled panel (no scroll — cards fit naturally) ──
        VBox contentPanel = new VBox();
        contentPanel.setAlignment(Pos.CENTER);
        contentPanel.getChildren().add(cardsBox);
        contentPanel.setStyle(
            "-fx-background-color: rgba(30, 15, 5, 0.60);" +
            "-fx-border-color: rgba(184, 150, 100, 0.45);" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;"
        );

        // ── Back button ──
        Button btnBack = new Button("Back to Menu");
        btnBack.setStyle(BTN_SEC_NORMAL);
        btnBack.setOnMouseEntered(e -> btnBack.setStyle(BTN_SEC_HOVER));
        btnBack.setOnMouseExited(e -> btnBack.setStyle(BTN_SEC_NORMAL));
        btnBack.setOnAction(e -> mainApp.showLandingPage());

        // ── Outer layout ──
        VBox outerContent = new VBox(16, titleStack, contentPanel, btnBack);
        outerContent.setAlignment(Pos.CENTER);
        outerContent.setPadding(new Insets(40));

        root.getChildren().add(outerContent);

        // ── Responsive resize ──
        root.widthProperty().addListener((obs, o, w) -> applyLayout(w.doubleValue(), root.getHeight(),
                titleShadow, titleFront, contentPanel));
        root.heightProperty().addListener((obs, o, h) -> applyLayout(root.getWidth(), h.doubleValue(),
                titleShadow, titleFront, contentPanel));
    }

    private static HBox buildDeveloperCard(String imagePath, String name, boolean photoRight) {
        HBox card = new HBox(24);
        card.setPadding(new Insets(3, 32, 3, 32));

        // ── Circular photo ──
        ImageView photo = new ImageView(UIUtils.ImageCache.get(imagePath));
        photo.setFitWidth(160);
        photo.setFitHeight(160);
        photo.setPreserveRatio(false);

        Circle clip = new Circle(80, 80, 80);
        photo.setClip(clip);

        StackPane photoPane = new StackPane(photo);
        photoPane.setMinSize(160, 160);
        photoPane.setMaxSize(160, 160);

        // ── Name label ──
        Label nameLabel = new Label(name);
        nameLabel.setStyle(
            "-fx-text-fill: #f0d9b5;" +
            "-fx-font-family: '" + UIUtils.MAIN_FONT + "';" +
            "-fx-font-size: 30px;" +
            "-fx-font-weight: bold;"
        );

        if (photoRight) {
            card.setAlignment(Pos.CENTER_RIGHT);
            card.getChildren().addAll(nameLabel, photoPane);
        } else {
            card.setAlignment(Pos.CENTER_LEFT);
            card.getChildren().addAll(photoPane, nameLabel);
        }

        return card;
    }

    private void applyLayout(double w, double h,
                             Label titleShadow, Label titleFront,
                             VBox contentPanel) {
        if (w <= 0 || h <= 0) return;

        double titleSz = clamp(w * 0.065, 28, 96);
        double sh = clamp(titleSz * 0.04, 2, 5);

        titleShadow.setFont(Font.font(UIUtils.MAIN_FONT, titleSz));
        titleFront.setFont(Font.font(UIUtils.MAIN_FONT, titleSz));
        titleShadow.setTranslateX(sh);
        titleShadow.setTranslateY(sh);

        double panelW = clamp(w * 0.50, 340, 800);
        contentPanel.setPrefWidth(panelW);
        contentPanel.setMaxWidth(panelW);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public javafx.scene.Parent getRoot() {
        return root;
    }
}