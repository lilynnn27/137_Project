package app.screens;

import app.Main;
import app.utils.UIUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

/**
 * LandingPageScreen — fully responsive at every window size.
 *
 * Strategy: content (title + subtitle + buttons) is placed in a VBox that
 * is centered inside the root StackPane, then shifted downward by 18 % of
 * the window height so it lands in the cream band of the background artwork.
 * Because the shift is a *fraction* of the current height it works correctly
 * at any resolution — small window, 1080p, 1440p, or maximised.
 * Font sizes and spacing all scale with window width, clamped to readable
 * min/max values so nothing gets comically large or unreadably small.
 */
public class LandingPageScreen {

    private final StackPane root;
    private Image bgImage;

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

        // ── Title (two stacked labels = shadow effect) ────────────────
        StackPane titleStack = new StackPane();
        titleStack.setAlignment(Pos.CENTER);

        Label titleShadow = new Label("DOUGHMINATION");
        titleShadow.setStyle("-fx-text-fill: #5D4037;");   // dark brown shadow

        Label titleFront = new Label("DOUGHMINATION");
        titleFront.setStyle("-fx-text-fill: #b89664;");    // gold

        titleStack.getChildren().addAll(titleShadow, titleFront);

        // ── Subtitle ─────────────────────────────────────────────────
        Label subtitle = new Label("A NETWORKED GAME PROJECT");
        subtitle.setStyle("-fx-text-fill: #795548;");

        // ── Buttons ──────────────────────────────────────────────────
        String normal = "-fx-background-color: transparent; -fx-text-fill: #5D4037; -fx-cursor: hand;";
        String hover  = "-fx-background-color: transparent; -fx-text-fill: #ff9900; -fx-cursor: hand;";

        Button btnSingle = makeBtn("Single Player", normal, hover);
        btnSingle.setOnAction(e -> mainApp.showSinglePlayer());

        Button btnMulti = makeBtn("Multiplayer", normal, hover);
        btnMulti.setOnAction(e -> mainApp.showMultiplayer());

        Button btnExit = makeBtn("Exit Game", normal, hover);
        btnExit.setOnAction(e -> mainApp.exitGame());

        HBox buttonRow = new HBox(0, btnSingle, btnMulti, btnExit);
        buttonRow.setAlignment(Pos.CENTER);

        // ── Content block (StackPane centers this; translateY nudges it) ──
        VBox content = new VBox(0, titleStack, subtitle, buttonRow);
        content.setAlignment(Pos.CENTER);

        root.getChildren().add(content);

        // ── Responsive listener ───────────────────────────────────────
        // Fires on every resize — including the initial layout pass when
        // the stage goes maximised. Both width and height listeners call
        // the same helper so either dimension change triggers a full re-layout.
        root.widthProperty().addListener((obs, o, w) ->
            applyLayout(w.doubleValue(), root.getHeight(),
                titleShadow, titleFront, subtitle,
                btnSingle, btnMulti, btnExit, buttonRow, content));

        root.heightProperty().addListener((obs, o, h) ->
            applyLayout(root.getWidth(), h.doubleValue(),
                titleShadow, titleFront, subtitle,
                btnSingle, btnMulti, btnExit, buttonRow, content));
    }

    // ── Layout logic ──────────────────────────────────────────────────────

    private void applyLayout(double w, double h, Label titleShadow, Label titleFront, Label subtitle, Button sp, Button mp, Button ex, HBox buttonRow, VBox content) {
        if (w <= 0 || h <= 0) return;

        // All sizes scale linearly with width, clamped to sane bounds
        double titleSz = clamp(w * 0.072, 28, 108);
        double subtitleSz = clamp(w * 0.032, 14, 52);
        double btnSz = clamp(w * 0.030, 13, 46);
        double btnGap = clamp(w * 0.028, 12, 52);
        double vGap = clamp(h * 0.016, 5, 20);

        titleShadow.setFont(Font.font(UIUtils.MAIN_FONT, titleSz));
        titleFront.setFont(Font.font(UIUtils.MAIN_FONT, titleSz));
        subtitle.setFont(Font.font(UIUtils.MAIN_FONT, subtitleSz));
        sp.setFont(Font.font(UIUtils.MAIN_FONT, btnSz));
        mp.setFont(Font.font(UIUtils.MAIN_FONT, btnSz));
        ex.setFont(Font.font(UIUtils.MAIN_FONT, btnSz));

        buttonRow.setSpacing(btnGap);
        content.setSpacing(vGap);

        // Shadow offset is proportional to font size
        double sh = clamp(titleSz * 0.04, 2, 5);
        titleShadow.setTranslateX(sh);
        titleShadow.setTranslateY(sh);

        content.setTranslateY(h * 0.075);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static Button makeBtn(String text, String normal, String hover) {
        Button b = new Button(text);
        b.setStyle(normal);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(normal));
        return b;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public javafx.scene.Parent getRoot() { return root; }
}