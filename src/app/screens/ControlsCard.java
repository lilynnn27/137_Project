package app.screens;

import app.utils.UIUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ControlsCard extends VBox {

    private static final String CARD_STYLE =
            "-fx-background-color: #3e2318;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #5a3825;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;";

    private static final String KEY_STYLE =
            "-fx-background-color: #2a1608;" +
            "-fx-background-radius: 6;" +
            "-fx-border-color: #7a5533;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 6;" +
            "-fx-text-fill: #f0d9b5;" +
            "-fx-padding: 4 10 4 10;";

    private static final String SUBLABEL_STYLE =
            "-fx-text-fill: #a0856a;" +
            "-fx-font-size: 25px;";

    public ControlsCard() {
        setSpacing(10);
        setPadding(new Insets(14, 16, 14, 16));
        setStyle(
                "-fx-background-color: #3a1e0e;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #5a3825;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;"
        );

        // ── Section header ──
        Label header = sectionHeader("CONTROLS");
        getChildren().add(header);

        // ── Three sub-cards row ──
        VBox moveCard   = buildMoveCard();
        VBox arrowCard  = buildArrowCard();
        VBox mouseCard  = buildMouseCard();

        HBox row = new HBox(10, moveCard, arrowCard, mouseCard);
        row.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(row);
    }

    private VBox buildMoveCard() {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setStyle(CARD_STYLE);
        card.setPrefWidth(160);

        Label title = subTitle("MOVE");

        // W key row
        HBox wRow = new HBox();
        wRow.setAlignment(Pos.CENTER);
        wRow.getChildren().add(keyLabel("W"));

        // A S D row
        HBox asdRow = new HBox(4, keyLabel("A"), keyLabel("S"), keyLabel("D"));
        asdRow.setAlignment(Pos.CENTER);

        card.getChildren().addAll(title, wRow, asdRow);
        return card;
    }

    private VBox buildArrowCard() {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setStyle(CARD_STYLE);
        card.setPrefWidth(160);

        Label title = subTitle("MOVE");

        // W key row
        HBox wRow = new HBox();
        wRow.setAlignment(Pos.CENTER);
        wRow.getChildren().add(keyLabel("↑"));

        // A S D row
        HBox asdRow = new HBox(4, keyLabel("←"), keyLabel("↓"), keyLabel("→"));
        asdRow.setAlignment(Pos.CENTER);

        card.getChildren().addAll(title, wRow, asdRow);
        return card;
    }

    private VBox buildMouseCard() {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setStyle(CARD_STYLE);
        card.setPrefWidth(140);

        Label title = subTitle("MOUSE CURSOR");

        // Mouse icon (unicode cursor approximation)
        Label mouseIcon = new Label("\uD83D\uDDB1");
        mouseIcon.setStyle("-fx-font-size: 28px;");

        card.getChildren().addAll(title, mouseIcon);
        return card;
    }

    // ── Helpers ──

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

    private static Label subTitle(String text) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-text-fill: #9e8060;" +
                "-fx-font-size: 11px;" +
                "-fx-font-family: '" + UIUtils.MAIN_FONT + "';"
        );
        return l;
    }

    private static Label keyLabel(String key) {
        Label l = new Label(key);
        l.setStyle(KEY_STYLE);
        l.setFont(javafx.scene.text.Font.font(UIUtils.MAIN_FONT, 13));
        return l;
    }
}
