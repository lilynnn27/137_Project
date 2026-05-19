package app.game_logic;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * Abstract base for all hazards and power-ups (H1–H3, P1–P3).
 *
 * Handles the shared pickup lifecycle:
 * spawn at position → render on canvas → collision detection → despawn on
 * contact.
 *
 */
public abstract class PickupEntity {

    protected final double x;
    protected final double y;
    private boolean active = true;
    private final Image sprite;

    private static final double SPRITE_SIZE = 80.0;
    private static final double COLLISION_RADIUS = 55.0;

    protected PickupEntity(double x, double y, Image sprite) {
        this.x = x;
        this.y = y;
        this.sprite = sprite;
    }

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    public boolean isActive() {
        return active;
    }

    public void despawn() {
        active = false;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    // -----------------------------------------------------------------------
    // Collision
    // -----------------------------------------------------------------------

    /** Returns true if the player centre at (px, py) overlaps this pickup. */
    public boolean isContactedBy(double px, double py) {
        return active && Math.hypot(px - x, py - y) < COLLISION_RADIUS;
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    /** Draws this pickup sprite on the overlay canvas. */
    public void draw(GraphicsContext gc) {
        if (!active || sprite == null)
            return;
        gc.drawImage(sprite,
                x - SPRITE_SIZE / 2,
                y - SPRITE_SIZE / 2,
                SPRITE_SIZE, SPRITE_SIZE);
    }

    // -----------------------------------------------------------------------
    // Effect metadata (subclasses implement)
    // -----------------------------------------------------------------------

    /** How long the effect lasts after pickup, in seconds. */
    public abstract double getEffectDurationSeconds();
}
