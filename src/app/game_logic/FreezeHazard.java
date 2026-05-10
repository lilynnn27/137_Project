package app.game_logic;

import javafx.scene.image.Image;

/**
 * H2 — Ice Spill Freeze Hazard (Erin).
 *
 * Completely freezes the contacting player's movement for 4 seconds (midpoint
 * of the 3–5 s spec). The player can still be eliminated while frozen —
 * collision detection in GamePlayScreen runs unconditionally regardless of
 * frozen state.
 *
 * Sprite: assets/images/hazard/Ice-Hazard.png
 */
public class FreezeHazard extends PickupEntity {

    /** Duration the freeze effect lasts, in seconds. */
    public static final double DURATION_SECONDS = 4.0;

    public FreezeHazard(double x, double y, Image sprite) {
        super(x, y, sprite);
    }

    @Override
    public double getEffectDurationSeconds() {
        return DURATION_SECONDS;
    }
}
