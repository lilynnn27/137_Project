package app.game_logic;

import javafx.scene.image.Image;

/**
 * P3 — Flour Transparent Trail Powerup
 *
 * Trail cannot be seen by enemies but you can see it, for 5 seconds.
 */
public class TransparentTrailPowerup extends PickupEntity {

    private static final double DURATION_SECONDS = 5.0;

    public TransparentTrailPowerup(double x, double y, Image sprite) {
        super(x, y, sprite);
    }

    @Override
    public double getEffectDurationSeconds() {
        return DURATION_SECONDS;
    }
}
