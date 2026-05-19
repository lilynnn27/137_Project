package app.game_logic;

import javafx.scene.image.Image;

/**
 * P1 — Oil Speed Powerup
 *
 * Increases the contacting player's movement speed by 1.5x for 5 seconds.
 */
public class SpeedPowerup extends PickupEntity {

    /**
     * Movement speed multiplier applied while powerup is active.
     */
    public static final double SPEED_MULTIPLIER = 1.50;

    private static final double DURATION_SECONDS = 5.0;

    public SpeedPowerup(double x, double y, Image sprite) {
        super(x, y, sprite);
    }

    @Override
    public double getEffectDurationSeconds() {
        return DURATION_SECONDS;
    }
}
