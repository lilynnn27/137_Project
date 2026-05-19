package app.utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.text.Font;

public class UIUtils {
    public static String MAIN_FONT;

    static {
        try {
            java.net.URL fontUrl = UIUtils.class.getResource("/assets/fonts/MainFont.ttf");
            if (fontUrl != null) {
                Font loadedFont = Font.loadFont(fontUrl.toExternalForm(), 12);
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

    public static final class ImageCache {
        private static final Map<String, Image> CACHE = new HashMap<>();

        private static final String[] PRELOAD_PATHS = {
            "assets/images/MainBackground.jpg",
            "assets/images/GameplayBackground.jpg",
            "assets/images/GameOverModal.png",
            "assets/images/PlayersDough/orange.png",
            "assets/images/PlayersDough/blue.png",
            "assets/images/PlayersDough/green.png",
            "assets/images/PlayersDough/red.png",
            "assets/images/hazard/RollingPin-Hazard.png",
            "assets/images/hazard/Ice-Hazard.png",
            "assets/images/hazard/RottenEgg-Hazard.png",
            "assets/images/powerup/Oil-Powerup.png",
            "assets/images/powerup/Dough-Powerup.png",
            "assets/images/powerup/Flour-Powerup.png"
        };

        public static void preload() {
            for (String path : PRELOAD_PATHS) {
                try {
                    java.net.URL url = UIUtils.class.getResource("/" + path);
                    if (url != null) {
                        CACHE.put(path, new Image(url.toExternalForm(), 1920, 1080, true, true, false));
                    }
                } catch(Exception e) {}
            }
        }

        public static Image get(String path) {
            Image img = CACHE.get(path);
            if (img != null && !img.isError()) return img;
            try {
                java.net.URL url = UIUtils.class.getResource("/" + path);
                if (url != null) {
                    img = new Image(url.toExternalForm(), 1920, 1080, true, true, false);
                    CACHE.put(path, img);
                }
            } catch(Exception e) {}
            return img;
        }
    }
}
