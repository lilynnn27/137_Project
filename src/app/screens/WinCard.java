package app.screens;

import app.utils.UIUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class WinCard extends VBox {

    public WinCard() {
        setSpacing(10);
        setPadding(new Insets(14, 16, 14, 16));
        setStyle(
                "-fx-background-color: #3a1e0e;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #5a3825;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;"
        );

        Label header = sectionHeader("HOW TO WIN");
        getChildren().add(header);

        // ── Win description tile ──
        HBox tile = new HBox(14);
        tile.setAlignment(Pos.CENTER_LEFT);
        tile.setPadding(new Insets(12, 14, 12, 14));
        tile.setStyle(
                "-fx-background-color: #2a1608;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: #7a5533;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 8;"
        );

        ImageView trophy = new ImageView(UIUtils.ImageCache.get("assets/images/trophy.png"));
        trophy.setFitWidth(50);
        trophy.setFitHeight(50);
        trophy.setPreserveRatio(true);

        Label desc = new Label(
                "When the timer runs out, the player with the most territory (%) wins. " +
                "If only one player remains before time's up, they win immediately. " +
                "In timed mode, eliminated players respawn after 3 seconds."
        );
        desc.setWrapText(true);
        desc.setStyle(
                "-fx-text-fill: #c8aa88;" +
                "-fx-font-family: '" + UIUtils.MAIN_FONT + "';" +
                "-fx-font-size: 18px;"
        );

        tile.getChildren().addAll(trophy, desc);
        getChildren().add(tile);
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
