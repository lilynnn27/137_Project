package app.game_logic;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the player's trail line drawn outside their territory.
 *
 * Rules:
 * - Trail only records points while the player is OUTSIDE their territory.
 * - Trail is drawn as a colored line matching the player's dough color.
 * - Self-collision (touching own trail) → player dies.
 * - Trail is cleared on successful capture OR player death.
 */
public class TrailManager {

  private final List<Point2D> points = new ArrayList<>();
  private boolean active = false;

  // Minimum pixel distance before a new point is added (avoids point spam)
  private static final double MIN_DIST = 6.0;

  // Width of the trail line
  private static final double LINE_WIDTH = 28.0;

  // How many tail points to skip when checking self-collision (avoids false
  // positives with the segment right behind the player head)
  private static final int SELF_COLLISION_SKIP = 4;

  // Distance threshold for self-collision detection (point-based, used by enemy check)
  private static final double SELF_COLLISION_RADIUS = 8.0;

  private double widthMultiplier = 1.0;

  public void setWidthMultiplier(double m) {
    this.widthMultiplier = m;
  }

  /**
   * Called every game frame.
   *
   * @param x                 Current player world X
   * @param y                 Current player world Y
   * @param isInsideTerritory Whether the player is currently inside their own
   *                          territory
   * @return {@code true} if the player just returned to their territory (capture
   *         event)
   */
  public boolean update(double x, double y, boolean isInsideTerritory) {
    if (isInsideTerritory) {
      if (active && points.size() >= 2) {
        // Player reconnected — capture happens; caller will handle territory expansion
        active = false;
        return true; // Capture!
      }
      // Was never outside, or trail too short — just reset
      active = false;
      points.clear();
      return false;
    }

    // Player is outside territory — record trail
    active = true;
    Point2D newPt = new Point2D(x, y);
    if (points.isEmpty() || points.get(points.size() - 1).distance(newPt) > MIN_DIST) {
      points.add(newPt);
    }
    return false;
  }

  /** Perpendicular distance from point {@code p} to segment {@code a→b}. */
  private static double distanceToSegment(Point2D p, Point2D a, Point2D b) {
    double dx = b.getX() - a.getX();
    double dy = b.getY() - a.getY();
    double lenSq = dx * dx + dy * dy;
    if (lenSq == 0) return p.distance(a);
    double t = Math.max(0, Math.min(1,
        ((p.getX() - a.getX()) * dx + (p.getY() - a.getY()) * dy) / lenSq));
    return p.distance(new Point2D(a.getX() + t * dx, a.getY() + t * dy));
  }

  /**
   * Check whether the player's head hits their own trail (self-collision).
   * Skips the most-recent {@code SELF_COLLISION_SKIP} points to avoid false
   * positives with the segment immediately behind the player.
   */
  public boolean checkSelfCollision(double playerX, double playerY) {
    if (points.size() < SELF_COLLISION_SKIP + 2)
      return false;

    double currentCollisionRadius = (LINE_WIDTH * widthMultiplier) / 2.0;

    Point2D head = new Point2D(playerX, playerY);
    int limit = points.size() - SELF_COLLISION_SKIP;
    for (int i = 0; i < limit - 1; i++) {
      if (distanceToSegment(head, points.get(i), points.get(i + 1)) < currentCollisionRadius) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check whether an enemy player's head touches this trail.
   * No skip zone — the full trail is a hazard to other players.
   *
   * @param px Enemy player world X
   * @param py Enemy player world Y
   * @return true if the enemy head is within collision radius of any trail point
   */
  public boolean checkEnemyCollision(double px, double py) {
    if (!active || points.size() < 2)
      return false;

    double currentCollisionRadius = (LINE_WIDTH * widthMultiplier) / 2.0;

    Point2D head = new Point2D(px, py);
    for (int i = 0; i < points.size() - 1; i++) {
      if (distanceToSegment(head, points.get(i), points.get(i + 1)) < currentCollisionRadius) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the current trail points (a snapshot copy).
   * Used by TerritoryManager to compute the enclosed polygon for capture.
   */
  public List<Point2D> getTrailPoints() {
    return new ArrayList<>(points);
  }

  /** Whether the trail is currently being recorded. */
  public boolean isActive() {
    return active;
  }

  /** Clears all trail data (call after capture or death). */
  public void clear() {
    points.clear();
    active = false;
  }

  /**
   * Draws the trail as a single polyline using the player's color.
   *
   * @param gc    The GraphicsContext (already translated so world (0,0) is
   *              correct)
   * @param color The player's dough color
   */
  private double[] cachedXs = new double[128];
  private double[] cachedYs = new double[128];

  public void draw(GraphicsContext gc, Color color) {
    if (points.size() < 2)
      return;

    gc.save();
    gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
    gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

    // Trail at 50% transparency in the player's dough color
    gc.setGlobalAlpha(0.5);
    gc.setStroke(color);
    gc.setLineWidth(LINE_WIDTH * widthMultiplier);
    
    int size = points.size();
    if (size > cachedXs.length) {
        int newCap = Math.max(cachedXs.length * 2, size);
        cachedXs = new double[newCap];
        cachedYs = new double[newCap];
    }
    
    for (int i = 0; i < size; i++) {
        cachedXs[i] = points.get(i).getX();
        cachedYs[i] = points.get(i).getY();
    }
    
    gc.strokePolyline(cachedXs, cachedYs, size);

    gc.restore();
  }
}