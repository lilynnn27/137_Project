package app.screens;

import java.io.File;
import java.util.List;

import app.Main;
import app.network.NetworkMessage.GameResult;
import app.utils.UIUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * GameOverModal
 *
 * Single-player: shows GAME OVER + territory % + player sprite + Retry/Menu.
 * Multiplayer  : shows YOU WIN / YOU LOSE / IT'S A TIE! + full ranked
 *                leaderboard with each player's dough sprite and territory %.
 *
 * Entirely responsive — modal size is derived from the primary stage so it
 * looks correct on any resolution.
 */
public class GameOverModal {

    // ── Theme colours ────────────────────────────────────────────────────
    private static final String C_GOLD       = "#b89664";
    private static final String C_SHADOW     = "#ba2b00";
    private static final String C_BROWN      = "#795548";
    private static final String C_DARK       = "#5D4037";
    private static final String C_ORANGE     = "#ff9900";
    private static final String C_HIGHLIGHT  = "#ffe9c0";
    private static final String C_ROW_ALT    = "rgba(255,248,238,0.55)";

    /** Maps server color hex → dough image name (no extension). */
    private static final java.util.Map<String, String> COLOR_TO_DOUGH =
        java.util.Map.of(
            "#FF7043", "orange",
            "#1E88E5", "blue",
            "#43A047", "green",
            "#E53935", "red",
            "#FDD835", "yellow",
            "#EC407A", "pink",
            "#8E24AA", "purple",
            "#3949AB", "indigo"
        );

    // ── State ────────────────────────────────────────────────────────────
    private final Main             mainApp;
    private final Stage            window;

    private final boolean          isMultiplayer;

    /**
     * How the game ended — controls the witty subtitle line.
     * TIMER  : time ran out (single-player always uses this)
     * LAST   : last player standing (someone died)
     * NORMAL : server triggered a normal end
     */
    public enum EndReason { TIMER, LAST_STANDING, NORMAL }
    private final EndReason endReason;
    private final List<GameResult> results;       // null in single-player
    private final int              myPlayerId;    // -1 in single-player
    private final double           myTerritoryPct;
    private final String           mySpritePath;  // e.g. "assets/images/PlayersDough/orange.png"

    private final Image bgImage;

    // ────────────────────────────────────────────────────────────────────
    // Constructors
    // ────────────────────────────────────────────────────────────────────

    /** Single-player constructor. */
    public GameOverModal(Main mainApp, double territoryPct, String spritePath) {
        this.mainApp        = mainApp;
        this.isMultiplayer  = false;
        this.results        = null;
        this.myPlayerId     = -1;
        this.myTerritoryPct = territoryPct;
        this.mySpritePath   = spritePath;
        this.endReason      = EndReason.TIMER;
        this.bgImage        = UIUtils.ImageCache.get("assets/images/GameOverModal.png");
        this.window         = buildStage();
    }

    /** Multiplayer constructor — receives ranked results list from the server. */
    public GameOverModal(Main mainApp, List<GameResult> results,
                         int myPlayerId, String mySpritePath) {
        this(mainApp, results, myPlayerId, mySpritePath, EndReason.TIMER);
    }

    /** Multiplayer constructor with explicit end reason. */
    public GameOverModal(Main mainApp, List<GameResult> results,
                         int myPlayerId, String mySpritePath, EndReason reason) {
        this.mainApp       = mainApp;
        this.isMultiplayer = true;
        this.results       = results;
        this.myPlayerId    = myPlayerId;
        this.mySpritePath  = mySpritePath;
        this.endReason     = reason;
        this.bgImage       = UIUtils.ImageCache.get("assets/images/GameOverModal.png");
        this.window        = buildStage();

        double pct = 0;
        for (GameResult r : results) {
            if (r.playerId == myPlayerId) { pct = r.territoryPercent; break; }
        }
        this.myTerritoryPct = pct;
    }

    // ────────────────────────────────────────────────────────────────────
    // Show
    // ────────────────────────────────────────────────────────────────────

    public void show() {
        mainApp.setBackgroundBlur(true);

        // Responsive: 75 % of primary stage, clamped
        double sw = mainApp.getPrimaryStage().getWidth();
        double sh = mainApp.getPrimaryStage().getHeight();
        double mw = clamp(sw * 0.75, 680, 1380);
        double mh = clamp(sh * 0.75, 400, 820);

        // ── Background image ─────────────────────────────────────────
        ImageView bg = new ImageView();
        if (bgImage != null && !bgImage.isError()) bg.setImage(bgImage);
        bg.setPreserveRatio(false);
        bg.fitWidthProperty().bind(window.widthProperty());
        bg.fitHeightProperty().bind(window.heightProperty());

        // ── Title ────────────────────────────────────────────────────
        double titleSz  = clamp(mw * 0.063, 34, 96);
        String titleTxt = isMultiplayer ? outcomeTitle() : "GAME OVER";
        StackPane titleStack = shadowLabel(titleTxt, titleSz);

        // ── Witty subtitle ───────────────────────────────────────────
        double subtitleSz = clamp(mw * 0.028, 14, 38);
        String subtitleTxt = wittySubtitle();
        Label subtitleLbl = new Label(subtitleTxt);
        subtitleLbl.setFont(Font.font(UIUtils.MAIN_FONT, subtitleSz));
        subtitleLbl.setStyle("-fx-text-fill: " + C_BROWN + "; -fx-font-style: italic;");
        subtitleLbl.setWrapText(true);
        subtitleLbl.setMaxWidth(mw * 0.80);
        subtitleLbl.setAlignment(Pos.CENTER);

        // ── Centre content ───────────────────────────────────────────
        VBox centre = isMultiplayer
            ? buildLeaderboard(mw, mh)
            : buildSingleScore(mw);

        // ── Buttons ──────────────────────────────────────────────────
        double btnSz = clamp(mw * 0.028, 16, 44);
        HBox btnRow  = new HBox(clamp(mw * 0.04, 16, 60));
        btnRow.setAlignment(Pos.CENTER);

        if (!isMultiplayer) {
            Button retry = btn("Retry", btnSz);
            retry.setOnAction(e -> { window.close(); mainApp.showGamePlay(); });
            btnRow.getChildren().add(retry);
        }
        Button menu = btn("Back to Menu", btnSz);
        menu.setOnAction(e -> { window.close(); mainApp.showLandingPage(); });
        btnRow.getChildren().add(menu);

        // ── Assemble ─────────────────────────────────────────────────
        double vGap = clamp(mh * 0.025, 8, 28);
        VBox content = new VBox(vGap, titleStack, subtitleLbl, centre, btnRow);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(mh * 0.05, mw * 0.06, mh * 0.05, mw * 0.06));

        StackPane root = new StackPane(bg, content);
        root.setBackground(null);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        window.setWidth(mw);
        window.setHeight(mh);
        window.setScene(scene);
        window.centerOnScreen();
        window.setOnHidden(e -> mainApp.setBackgroundBlur(false));
        window.show();
    }

    // ────────────────────────────────────────────────────────────────────
    // Single-player score panel
    // ────────────────────────────────────────────────────────────────────

    private VBox buildSingleScore(double mw) {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);

        // Player sprite
        Image sprite = loadSprite(mySpritePath);
        if (sprite != null) {
            ImageView iv = new ImageView(sprite);
            double sz = clamp(mw * 0.09, 64, 130);
            iv.setFitWidth(sz);
            iv.setFitHeight(sz);
            iv.setPreserveRatio(true);
            box.getChildren().add(iv);
        }

        double scoreSz = clamp(mw * 0.030, 18, 50);
        Label lbl = new Label(String.format("Final Territory: %.1f%%", myTerritoryPct));
        lbl.setFont(Font.font(UIUtils.MAIN_FONT, scoreSz));
        lbl.setStyle("-fx-text-fill: " + C_BROWN + ";");
        box.getChildren().add(lbl);
        return box;
    }

    // ────────────────────────────────────────────────────────────────────
    // Multiplayer leaderboard
    // ────────────────────────────────────────────────────────────────────

    private VBox buildLeaderboard(double mw, double mh) {
        VBox board = new VBox(clamp(mh * 0.012, 5, 14));
        board.setAlignment(Pos.CENTER);
        board.setMaxWidth(mw * 0.9);

        double rowFont = clamp(mw * 0.026, 13, 36);

        for (GameResult r : results) {
            board.getChildren().add(buildRow(r, mw, rowFont));
        }
        return board;
    }

    private HBox buildRow(GameResult r, double mw, double fontSize) {
        boolean isMe = (r.playerId == myPlayerId);
        boolean isWinner = (r.rank == 1);

        HBox row = new HBox(clamp(mw * 0.016, 8, 22));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 14, 6, 14));
        row.setMaxWidth(mw * 0.88);
        row.setStyle(
            "-fx-background-color: " + (isMe ? C_HIGHLIGHT : C_ROW_ALT) + ";" +
            "-fx-background-radius: 8;");

        // Rank medal
        String medal = switch (r.rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "#" + r.rank;
        };
        Label rankLbl = new Label(medal);
        rankLbl.setFont(Font.font(UIUtils.MAIN_FONT, clamp(fontSize * 1.1, 14, 42)));
        rankLbl.setStyle("-fx-text-fill: " + C_DARK + ";");
        rankLbl.setMinWidth(clamp(mw * 0.042, 28, 54));

        // Dough sprite in colour-tinted circle
        double spriteSz = clamp(fontSize * 2.1, 32, 64);
        StackPane spritePane = new StackPane();
        spritePane.setMinWidth(spriteSz + 8);
        spritePane.setMinHeight(spriteSz + 8);

        Circle disc = new Circle(spriteSz / 2 + 3);
        try {
            disc.setFill(Color.web(r.colorHex, 0.22));
            disc.setStroke(Color.web(r.colorHex));
        } catch (Exception ignored) {
            disc.setFill(Color.LIGHTGRAY);
        }
        disc.setStrokeWidth(2);
        spritePane.getChildren().add(disc);

        String doughPath = "assets/images/PlayersDough/" +
            COLOR_TO_DOUGH.getOrDefault(r.colorHex, "orange") + ".png";
        Image sprite = loadSprite(doughPath);
        if (sprite != null) {
            ImageView iv = new ImageView(sprite);
            iv.setFitWidth(spriteSz);
            iv.setFitHeight(spriteSz);
            iv.setPreserveRatio(true);
            spritePane.getChildren().add(iv);
        }

        // Crown on winner
        if (isWinner) {
            Label crown = new Label("👑");
            crown.setFont(Font.font(clamp(spriteSz * 0.45, 10, 28)));
            crown.setTranslateY(-(spriteSz / 2 + 5));
            spritePane.getChildren().add(crown);
        }

        // Player name
        String nameText = r.playerName + (isMe ? " (You)" : "");
        Label nameLbl = new Label(nameText);
        nameLbl.setFont(Font.font(UIUtils.MAIN_FONT,
            clamp(isWinner ? fontSize * 1.05 : fontSize * 0.95, 12, 34)));
        nameLbl.setStyle("-fx-text-fill: " + (isMe ? C_DARK : C_BROWN) + ";" +
                         (isWinner ? "-fx-font-weight: bold;" : ""));

        // Spacer + territory %
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label pctLbl = new Label(String.format("%.1f%%", r.territoryPercent));
        pctLbl.setFont(Font.font(UIUtils.MAIN_FONT, clamp(fontSize * 0.9, 11, 30)));
        pctLbl.setStyle("-fx-text-fill: " + C_BROWN + ";");

        row.getChildren().addAll(rankLbl, spritePane, nameLbl, spacer, pctLbl);
        return row;
    }

    // ────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────

    private String outcomeTitle() {
        if (!isMultiplayer) return "GAME OVER";
        if (results == null || results.isEmpty()) return "GAME OVER";
        GameResult mine = null;
        for (GameResult r : results) if (r.playerId == myPlayerId) { mine = r; break; }
        if (mine == null) return "GAME OVER";
        long tiedFirst = results.stream().filter(r -> r.rank == 1).count();
        if (mine.rank == 1 && tiedFirst > 1) return "IT'S A TIE!";
        return mine.rank == 1 ? "YOU WIN!" : "YOU LOSE";
    }

    private String wittySubtitle() {
        if (!isMultiplayer) {
            if (myTerritoryPct >= 60) return "You kneaded that dough into submission. Bakery legend.";
            if (myTerritoryPct >= 35) return "Time's up! Not bad for a half-baked effort.";
            if (myTerritoryPct >= 15) return "The oven timer went off early.";
            if (myTerritoryPct >= 5) return "The tray is mostly empty…";
            return "The dough has spoken. Maybe it's time to consider a different hobby?";
        }

        // Multiplayer
        if (results == null || results.isEmpty()) return "The dough has spoken.";

        GameResult mine = null;
        for (GameResult r : results) if (r.playerId == myPlayerId) { mine = r; break; }
        if (mine == null) return "The dough has spoken.";

        long tiedFirst = results.stream().filter(r -> r.rank == 1).count();
        int  total     = results.size();
        boolean isLast = (mine.rank == total);

        // ── Winner ───────────────────────────────────────────────────
        if (mine.rank == 1) {
            if (tiedFirst > 1) {
                // Tie
                return "A doughmination standoff! The tray couldn't pick just one champion.";
            }
            if (endReason == EndReason.LAST_STANDING) {
                return "Last dough standing. The others crumbled under the pressure.";
            }
            if (myTerritoryPct >= 55) return "Absolute doughmination. The tray bows to its new overlord.";
            if (myTerritoryPct >= 35) return "Time's up! Our top doughminator claims the tray!";
            return "A slim victory, but the tray is yours. Don't push your luck.";
        }

        // ── Runner-up (2nd place) ─────────────────────────────────────
        if (mine.rank == 2) {
            if (endReason == EndReason.LAST_STANDING)
                return "So close… you were the second-to-last dough standing.";
            return "Silver-dusted but not forgotten. The filling dreams of a rematch.";
        }

        // ── Last place ───────────────────────────────────────────────
        if (isLast) {
            if (endReason == EndReason.LAST_STANDING)
                return "You got flattened first. The rolling pin shows no mercy.";
            if (myTerritoryPct < 5) return "You were basically a crumb on someone else's tray.";
            return "Dead last, at least you showed up to the kitchen.";
        }

        // ── Mid-pack ─────────────────────────────────────────────────
        if (endReason == EndReason.LAST_STANDING)
            return "Outlasted some, but not enough. The dough decides your fate.";
        return "A respectable performance. The dough gods are… mildly impressed.";
    }

    private StackPane shadowLabel(String text, double size) {
        Label shadow = new Label(text);
        shadow.setFont(Font.font(UIUtils.MAIN_FONT, size));
        shadow.setStyle("-fx-text-fill: " + C_SHADOW + ";");
        shadow.setTranslateX(3); shadow.setTranslateY(3);

        Label front = new Label(text);
        front.setFont(Font.font(UIUtils.MAIN_FONT, size));
        front.setStyle("-fx-text-fill: " + C_GOLD + ";");

        StackPane sp = new StackPane(shadow, front);
        sp.setAlignment(Pos.CENTER);
        return sp;
    }

    private Button btn(String text, double fontSize) {
        String normal = "-fx-background-color: transparent; -fx-text-fill: " + C_DARK + "; -fx-cursor: hand;";
        String hover = "-fx-background-color: transparent; -fx-text-fill: " + C_ORANGE + "; -fx-cursor: hand;";
        Button b = new Button(text);
        b.setFont(Font.font(UIUtils.MAIN_FONT, fontSize));
        b.setStyle(normal);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(normal));
        return b;
    }

    private static Stage buildStageStatic(Main mainApp) {
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.initStyle(StageStyle.TRANSPARENT);
        s.initOwner(mainApp.getPrimaryStage());
        return s;
    }

    private Stage buildStage() { return buildStageStatic(mainApp); }

    private static Image loadSprite(String path) {
        if (path == null) return null;
        Image cached = UIUtils.ImageCache.get(path);
        if (cached != null && !cached.isError()) return cached;
        File f = new File(path);
        return f.exists() ? new Image(f.toURI().toString()) : null;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}