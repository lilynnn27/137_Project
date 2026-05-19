package app.screens;

import app.utils.UIUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class PickupsCard extends VBox {

    public PickupsCard() {
        setSpacing(10);
        setPadding(new Insets(14, 16, 14, 16));
        setStyle(
                "-fx-background-color: #3a1e0e;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #5a3825;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;"
        );

        Label header = sectionHeader("HAZARDS & POWER-UPS (Activate on contact)");
        getChildren().add(header);

        // Hazards row (red/orange tones)
        FlowPane flow = new FlowPane();
        flow.setHgap(8);
        flow.setVgap(8);
        flow.setAlignment(Pos.CENTER_LEFT);

        String[][] pickups = {
            { "assets/images/hazard/RollingPin-Hazard.png",  "Rolling Pin — -30% speed",             "#8b2020" },
            { "assets/images/hazard/Ice-Hazard.png", "Ice spill — freeze",                "#1a4a6b" },
            { "assets/images/hazard/RottenEgg-Hazard.png","Rotten egg — reverse controls",     "#7a3800" },
            { "assets/images/powerup/Oil-Powerup.png",      "Spilled oil — 1.5x speed",          "#1a5c1a" },
            { "assets/images/powerup/Dough-Powerup.png",    "Dough — wider trail",               "#6b4c00" },
            { "assets/images/powerup/Flour-Powerup.png",    "Flour — invisible trail to enemies", "#2a5a2a" }
        };

        for (String[] p : pickups) {
            flow.getChildren().add(buildPickupChip(p[0], p[1], p[2]));
        }

        getChildren().add(flow);
    }

    private static HBox buildPickupChip(String imagePath, String text, String bgColor) {
        ImageView icon = new ImageView(UIUtils.ImageCache.get(imagePath));
        icon.setFitWidth(32);
        icon.setFitHeight(32);
        icon.setPreserveRatio(true);

        Label textLabel = new Label(text);
        textLabel.setStyle(
            "-fx-text-fill: #f0d9b5;" +
            "-fx-font-family: '" + UIUtils.MAIN_FONT + "';" +
            "-fx-font-size: 18px;"
        );

        HBox chip = new HBox(6, icon, textLabel);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-background-radius: 20;" +
            "-fx-padding: 5 14 10 10;"
        );
        return chip;
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
