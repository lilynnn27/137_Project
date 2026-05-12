package app.game_hud;

import java.util.List;

import app.utils.UIUtils;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class StatOverlay {

    public static class PlayerEntry {
        public final String name;
        public final Color  color;
        public final double territoryPercent;

        public PlayerEntry(String name, Color color, double territoryPercent) {
            this.name             = name;
            this.color            = color;
            this.territoryPercent = territoryPercent;
        }
    }

    private static final int MAX_PLAYERS = 4;

    private final VBox    root;
    private final Label[] rankLabels = new Label[MAX_PLAYERS];
    private final Label[] nameLabels = new Label[MAX_PLAYERS];
    private final Label[] pctLabels  = new Label[MAX_PLAYERS];
    private final HBox[]  rows       = new HBox[MAX_PLAYERS];

    public StatOverlay() {
        root = new VBox(4);
        root.setStyle(
            "-fx-background-color: rgba(0,0,0,0.55);" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 10 16 10 16;");
        root.setLayoutX(14);
        root.setLayoutY(14);
        root.setPrefWidth(250);

        Label header = new Label("Leaderboard");
        header.setFont(Font.font(UIUtils.MAIN_FONT, 20));
        header.setStyle("-fx-text-fill: #f0d090;");
        root.getChildren().add(header);

        for (int i = 0; i < MAX_PLAYERS; i++) {
            rankLabels[i] = new Label("#" + (i + 1));
            rankLabels[i].setFont(Font.font(UIUtils.MAIN_FONT, 16));
            rankLabels[i].setStyle("-fx-text-fill: #aaaaaa;");
            rankLabels[i].setPrefWidth(30);

            nameLabels[i] = new Label("—");
            nameLabels[i].setFont(Font.font(UIUtils.MAIN_FONT, 16));
            nameLabels[i].setStyle("-fx-text-fill: white;");
            nameLabels[i].setPrefWidth(140);

            pctLabels[i] = new Label("0.0%");
            pctLabels[i].setFont(Font.font(UIUtils.MAIN_FONT, 16));
            pctLabels[i].setStyle("-fx-text-fill: white;");

            rows[i] = new HBox(8, rankLabels[i], nameLabels[i], pctLabels[i]);
            rows[i].setAlignment(Pos.CENTER_LEFT);
            rows[i].setVisible(false);
            root.getChildren().add(rows[i]);
        }
    }

    /** Refreshes the leaderboard. Call every game tick. Entries must be pre-sorted descending by territoryPercent. */
    public void update(List<PlayerEntry> entries) {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (i < entries.size()) {
                PlayerEntry e = entries.get(i);
                rows[i].setVisible(true);
                nameLabels[i].setText(e.name);
                nameLabels[i].setStyle("-fx-text-fill: " + toHex(e.color) + ";");
                pctLabels[i].setText(String.format("%.1f%%", e.territoryPercent));
            } else {
                rows[i].setVisible(false);
            }
        }
    }

    private static String toHex(Color c) {
        return String.format("#%02X%02X%02X",
            (int) (c.getRed()   * 255),
            (int) (c.getGreen() * 255),
            (int) (c.getBlue()  * 255));
    }

    public Node getRoot() { return root; }
}
