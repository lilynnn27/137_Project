package app.game_logic;

import javafx.scene.image.Image;

/**
 * H1 — Rolling Pin Slowing Hazard
 *
 * Slows the contacting player's movement by 30% for 1.5 seconds.
 * Sprite: assets/images/hazard/RollingPin-Hazard.png
 *
 * Effect application is handled by GamePlayScreen, which reads
 * SPEED_MULTIPLIER and getEffectDurationSeconds() after despawn.
 */
public class SlowingHazard extends PickupEntity {

    /**
     * Movement speed multiplier applied while slow is active (0.70 = 30% slower).
     */
    public static final double SPEED_MULTIPLIER = 0.70;

    private static final double DURATION_SECONDS = 1.5;

    public SlowingHazard(double x, double y, Image sprite) {
        super(x, y, sprite);
    }

    @Override
    public double getEffectDurationSeconds() {
        return DURATION_SECONDS;
    }
}
