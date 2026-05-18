package app.screens;

import app.Main;
import app.utils.UIUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class RulesPage {
    private final StackPane root;

    public RulesPage(Main mainApp) {

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

        Label titleShadow = new Label("HOW TO PLAY");
        titleShadow.setStyle("-fx-text-fill: #5D4037;");
        titleShadow.setFont(Font.font(UIUtils.MAIN_FONT, 72));
        titleShadow.setTranslateX(3);
        titleShadow.setTranslateY(3);

        Label titleFront = new Label("HOW TO PLAY");
        titleFront.setStyle("-fx-text-fill: #b89664;");
        titleFront.setFont(Font.font(UIUtils.MAIN_FONT, 72));

        titleStack.getChildren().addAll(titleShadow, titleFront);

        // ── Translucent content panel ──
        Region contentPanel = new Region();
        contentPanel.setPrefSize(700, 420);
        contentPanel.setMaxSize(700, 420);
        contentPanel.setStyle(
            "-fx-background-color: rgba(30, 15, 5, 0.60);" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(184, 150, 100, 0.45);" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 12;"
        );

        // ── Back button ──
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: #b89664;"
                   + "-fx-font-family: '" + UIUtils.MAIN_FONT + "'; -fx-font-size: 16px; -fx-cursor: hand;";
        String hoverStyle  = "-fx-background-color: transparent; -fx-text-fill: #ff9900;"
                        + "-fx-font-family: '" + UIUtils.MAIN_FONT + "'; -fx-font-size: 16px; -fx-cursor: hand;";
        Button btnBack = new Button("\u2190 Back to Menu");
        btnBack.setFont(Font.font(UIUtils.MAIN_FONT, 16));  // keep this too as a fallback
        btnBack.setStyle(normalStyle);
        btnBack.setOnMouseEntered(e -> btnBack.setStyle(hoverStyle));
        btnBack.setOnMouseExited(e -> btnBack.setStyle(normalStyle));
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

    private void applyLayout(double w, double h,
                             Label titleShadow, Label titleFront,
                             Region contentPanel) {
        if (w <= 0 || h <= 0) return;

        double titleSz = clamp(w * 0.065, 28, 96);
        double subSz   = clamp(w * 0.022, 14, 36);
        double sh       = clamp(titleSz * 0.04, 2, 5);

        titleShadow.setFont(Font.font(UIUtils.MAIN_FONT, titleSz));
        titleFront.setFont(Font.font(UIUtils.MAIN_FONT, titleSz));
        titleShadow.setTranslateX(sh);
        titleShadow.setTranslateY(sh);
        // pageSubtitle.setFont(Font.font(UIUtils.MAIN_FONT, subSz));

        double panelW = clamp(w * 0.60, 400, 800);
        double panelH = clamp(h * 0.45, 280, 520);
        contentPanel.setPrefSize(panelW, panelH);
        contentPanel.setMaxSize(panelW, panelH);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public javafx.scene.Parent getRoot() {
        return root;
    }
}