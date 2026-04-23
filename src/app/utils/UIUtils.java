package app.utils;

import javafx.scene.control.Button;

public class UIUtils {
    // Reusable UI Styles (Black & White Theme)
    public static final String BUTTON_STYLE = "-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 2px; -fx-font-size: 20px; -fx-padding: 15 40; -fx-cursor: hand;";
    public static final String BUTTON_HOVER_STYLE = "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: white; -fx-border-width: 2px; -fx-font-size: 20px; -fx-padding: 15 40; -fx-cursor: hand;";
    public static final String BG_STYLE = "-fx-background-color: #000000;";
    public static final String TITLE_STYLE = "-fx-text-fill: #FFFFFF; -fx-font-size: 56px; -fx-font-weight: bold; -fx-letter-spacing: 2px;";
    public static final String SUBTITLE_STYLE = "-fx-text-fill: #AAAAAA; -fx-font-size: 24px;";

    // Helper method to add smooth hover effects to buttons
    public static void styleButton(Button btn, String normalStyle, String hoverStyle) {
        btn.setStyle(normalStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(normalStyle));
    }
}
