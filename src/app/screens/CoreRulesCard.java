package app.screens;

import app.utils.UIUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CoreRulesCard extends VBox {

    public CoreRulesCard() {
        setSpacing(10);
        setPadding(new Insets(14, 16, 14, 16));
        setStyle(
                "-fx-background-color: #3a1e0e;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #5a3825;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;"
        );

        Label header = sectionHeader("CORE RULES");
        getChildren().add(header);

        // 2-column grid of rule tiles
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col, col);

        String[][] rules = {
            { "assets/images/PlayersDough/pink.png", "Leave a trail",
              "Your empanada always moves and leaves a dough trail behind as you roam outside your territory." },
            { "assets/images/loop.png", "Close a loop",
              "Return to your own territory to enclose an area — all tiles inside become yours!" },
            { "assets/images/trail-protection.png", "Protect your trail",
              "If an enemy crosses your trail while you're outside, you're eliminated. Your territory vanishes!" },
            { "assets/images/safe-inside.png", "Safe inside",
              "Moving within your own territory is safe — no trail is laid and you can't be cut." },
            { "assets/images/steal-territory.png", "Steal territory",
              "Enclose an enemy's territory with a loop to claim it as your own." },
            { "assets/images/dont-cross.png", "Don't cross yourself",
              "Crossing your own trail outside your territory also eliminates you — watch your path!" }
        };

        for (int i = 0; i < rules.length; i++) {
            VBox tile = buildRuleTile(rules[i][0], rules[i][1], rules[i][2]);
            grid.add(tile, i % 2, i / 2);
        }

        getChildren().add(grid);
    }

    private static VBox buildRuleTile(String icon, String title, String desc) {
        VBox tile = new VBox(5);
        tile.setPadding(new Insets(10, 12, 10, 12));
        tile.setStyle(
                "-fx-background-color: #3e2318;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: #5a3825;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 8;"
        );

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        ImageView iconView = new ImageView(UIUtils.ImageCache.get(icon));
        iconView.setFitWidth(32);
        iconView.setFitHeight(32);
        iconView.setPreserveRatio(true);

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-text-fill: #f0d9b5;" +
                "-fx-font-family: '" + UIUtils.MAIN_FONT + "';" +
                "-fx-font-size: 25px;" +
                "-fx-font-weight: bold;"
        );

        titleRow.getChildren().addAll(iconView, titleLabel);

        Label descLabel = new Label(desc);
        descLabel.setWrapText(true);
        descLabel.setStyle(
                "-fx-text-fill: #b09878;" +
                "-fx-font-family: '" + UIUtils.MAIN_FONT + "';" +
                "-fx-font-size: 18px;"
        );

        tile.getChildren().addAll(titleRow, descLabel);
        return tile;
    }

    private static Label sectionHeader(String text) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-text-fill: #c8a96e;" +
                "-fx-font-family: '" + UIUtils.MAIN_FONT + "';" +
                "-fx-font-size: 25px;" +
                "-fx-font-weight: bold;"
        );
        return l;
    }

}
