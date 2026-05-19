package app.game_logic;

import javafx.scene.image.Image;

/**
 * H3 — Rotten Egg Reverse Controls Hazard (Emman).
 *
 * Inverts all player input for 3 seconds. 
 *
 */
public class ReverseControlsHazard extends PickupEntity {

    /** Duration the reverse-controls effect lasts, in seconds. */
    public static final double DURATION_SECONDS = 3.0;

    public ReverseControlsHazard(double x, double y, Image sprite) {
        super(x, y, sprite);
    }

    @Override
    public double getEffectDurationSeconds() {
        return DURATION_SECONDS;
    }
}
