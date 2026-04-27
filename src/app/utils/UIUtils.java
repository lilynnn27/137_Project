package app.utils;

import javafx.scene.control.Button;
import javafx.scene.text.Font;
import java.io.File;
import java.io.FileInputStream;

public class UIUtils {
    public static String MAIN_FONT;

    static {
        try {
            File fontFile = new File("assets/fonts/MainFont.ttf");
            if (fontFile.exists()) {
                Font loadedFont = Font.loadFont(new FileInputStream(fontFile), 12);
                MAIN_FONT = loadedFont.getFamily(); 
            } else {
                MAIN_FONT = "Arial";
            }
        } catch (Exception e) {
            MAIN_FONT = "Arial";
        }
    }

    public static final String BUTTON_STYLE = "-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 2px; -fx-padding: 15 40; -fx-cursor: hand;";
    public static final String BUTTON_HOVER_STYLE = "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: white; -fx-border-width: 2px; -fx-padding: 15 40; -fx-cursor: hand;";
    public static final String BG_STYLE = "-fx-background-color: #000000;";
    public static final String TITLE_STYLE = "-fx-text-fill: #FFFFFF; -fx-letter-spacing: 2px;";
    public static final String SUBTITLE_STYLE = "-fx-text-fill: #AAAAAA;";

    public static void styleButton(Button btn, String normalStyle, String hoverStyle) {
        btn.setFont(Font.font(MAIN_FONT, 20));
        btn.setStyle(normalStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(normalStyle));
    }
}