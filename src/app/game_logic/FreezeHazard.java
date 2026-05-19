package app.game_logic;

import javafx.scene.image.Image;

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
