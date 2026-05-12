package app.game_logic;

import javafx.scene.image.Image;

/**
 * P2 — Dough Bigger Size Powerup
 *
 * Increases the contacting player's trail size / coverage for 5 seconds.
 * Sprite: assets/images/powerup/Dough-Powerup.png
 */
public class BiggerSizePowerup extends PickupEntity {

    /**
     * Trail size multiplier applied while powerup is active.
     */
    public static final double TRAIL_SIZE_MULTIPLIER = 2.0;

    private static final double DURATION_SECONDS = 5.0;

    public BiggerSizePowerup(double x, double y, Image sprite) {
        super(x, y, sprite);
    }

    @Override
    public double getEffectDurationSeconds() {
        return DURATION_SECONDS;
    }
}
