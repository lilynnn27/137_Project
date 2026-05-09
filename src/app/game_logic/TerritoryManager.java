package app.game_logic;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the player's owned territory as a filled polygon.
 *
 * Territory is represented as an ordered list of 2-D vertices.
 *
 * Capture logic:
 * When the player returns to their territory after leaving a trail, the trail
 * points are MERGED with the territory boundary to form a new, larger polygon.
 * The simplest correct approach: the new polygon is the convex / concave hull
 * formed by the old territory + trail. For Splix-style gameplay we use a
 * simpler "stitch" approach: find where the trail's endpoints touch the
 * boundary and replace the boundary segment between those two touch-points
 * with the trail, choosing the side that ADDS area.
 *
 * Initial territory:
 * A small square / diamond centred on the player spawn.
 */
public class TerritoryManager {

  /** The current territory polygon vertices, in order. */
  private List<Point2D> polygon = new ArrayList<>();

  // -----------------------------------------------------------------------
  // Initialisation
  // -----------------------------------------------------------------------

  /**
   * Creates the starting territory: a circle-shaped polygon centred on (cx, cy).
   * Uses 32 vertices to closely approximate the round dough shape.
   *
   * @param cx     Centre X of spawn point (world coordinates)
   * @param cy     Centre Y of spawn point
   * @param radius Radius of the starting circle
   */
  public void initStartingTerritory(double cx, double cy, double radius) {
    polygon.clear();
    int SIDES = 32; // enough sides to look like a smooth circle
    for (int i = 0; i < SIDES; i++) {
      double angle = 2 * Math.PI * i / SIDES; // clockwise
      polygon.add(new Point2D(
          cx + radius * Math.cos(angle),
          cy + radius * Math.sin(angle)));
    }
  }

  // -----------------------------------------------------------------------
  // Territory queries
  // -----------------------------------------------------------------------

  /**
   * Returns {@code true} if the point (px, py) is inside the territory polygon.
   * Uses the ray-casting algorithm.
   */
  public boolean isInsideTerritory(double px, double py) {
    if (polygon.size() < 3)
      return false;

    int n = polygon.size();
    boolean inside = false;
    double x = px, y = py;

    for (int i = 0, j = n - 1; i < n; j = i++) {
      double xi = polygon.get(i).getX(), yi = polygon.get(i).getY();
      double xj = polygon.get(j).getX(), yj = polygon.get(j).getY();

      boolean intersect = ((yi > y) != (yj > y)) &&
          (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
      if (intersect)
        inside = !inside;
    }
    return inside;
  }

  // -----------------------------------------------------------------------
  // Capture
  // -----------------------------------------------------------------------

  /**
   * Expands the territory by merging the trail into the boundary polygon.
   * Territory is permanent — it only ever grows, never shrinks.
   *
   * Algorithm (Splix-style "stitch"):
   * 1. Use the trail's first/last points to find the nearest boundary vertices
   * (exit stitch and entry stitch).
   * 2. Build two candidate polygons: one going each way around the boundary,
   * with the trail appended to close the loop without self-intersection.
   * 3. Accept the largest candidate that is ≥ the current territory area.
   *
   * @param trail The recorded trail points (index 0 = near exit, last = near
   *              entry).
   */
  public void captureTerritory(List<Point2D> trail) {
    if (polygon.size() < 3 || trail.size() < 2)
      return;

    // Use the trail's own endpoints to locate the stitch vertices on the boundary.
    // This is more accurate than the approximate exitX/exitY passed from the game
    // loop.
    Point2D trailStart = trail.get(0); // near where player exited
    Point2D trailEnd = trail.get(trail.size() - 1); // near where player re-entered

    int exitIdx = nearestVertexIndex(trailStart.getX(), trailStart.getY());
    int entryIdx = nearestVertexIndex(trailEnd.getX(), trailEnd.getY());

    if (exitIdx == entryIdx) {
      // Both endpoints snapped to the same vertex. Use the second-nearest vertex
      // for the entry so the stitch has two distinct boundary points to work with.
      entryIdx = nearestVertexIndex(trailEnd.getX(), trailEnd.getY(), exitIdx);
    }

    double currentArea = Math.abs(signedArea(polygon));

    // Candidate A: walk boundary exit→entry (one arc), then trail REVERSED
    // (entry→exit).
    // The reversal is required so the polygon closes correctly without
    // self-intersection.
    List<Point2D> candA = buildCandidatePolygon(exitIdx, entryIdx, trail, true);

    // Candidate B: walk boundary entry→exit (the other arc), then trail FORWARD
    // (exit→entry).
    List<Point2D> candB = buildCandidatePolygon(entryIdx, exitIdx, trail, false);

    double areaA = Math.abs(signedArea(candA));
    double areaB = Math.abs(signedArea(candB));

    // Territory is permanent: only accept the result if it is at least as large
    // as the current territory. Pick the largest qualifying candidate.
    List<Point2D> best = polygon; // default: keep existing (no shrink)
    double bestArea = currentArea;

    if (areaA >= bestArea) {
      best = candA;
      bestArea = areaA;
    }
    if (areaB >= bestArea) {
      best = candB;
    } // bestArea already updated

    polygon = best;
  }

  /** Build one candidate polygon by stitching a boundary arc + trail. */
  private List<Point2D> buildCandidatePolygon(int fromIdx, int toIdx,
      List<Point2D> trail,
      boolean reverseTrail) {
    int n = polygon.size();
    List<Point2D> result = new ArrayList<>();

    // Walk the boundary from fromIdx to toIdx (wrapping around)
    int i = fromIdx;
    while (true) {
      result.add(polygon.get(i));
      if (i == toIdx)
        break;
      i = (i + 1) % n;
    }

    // Append the trail
    if (reverseTrail) {
      for (int t = trail.size() - 1; t >= 0; t--) {
        result.add(trail.get(t));
      }
    } else {
      result.addAll(trail);
    }

    return result;
  }

  /** Shoelace formula for signed area — positive = CCW, negative = CW. */
  private double signedArea(List<Point2D> pts) {
    double area = 0;
    int n = pts.size();
    for (int i = 0; i < n; i++) {
      Point2D a = pts.get(i);
      Point2D b = pts.get((i + 1) % n);
      area += (a.getX() * b.getY()) - (b.getX() * a.getY());
    }
    return area / 2.0;
  }

  /** Returns the index of the polygon vertex closest to (x, y). */
  private int nearestVertexIndex(double x, double y) {
    int best = 0;
    double bestDist = Double.MAX_VALUE;
    for (int i = 0; i < polygon.size(); i++) {
      double d = polygon.get(i).distance(x, y);
      if (d < bestDist) {
        bestDist = d;
        best = i;
      }
    }
    return best;
  }

  /** Same as above but skips one index — used to find a second-nearest vertex. */
  private int nearestVertexIndex(double x, double y, int exclude) {
    int best = -1;
    double bestDist = Double.MAX_VALUE;
    for (int i = 0; i < polygon.size(); i++) {
      if (i == exclude) continue;
      double d = polygon.get(i).distance(x, y);
      if (d < bestDist) {
        bestDist = d;
        best = i;
      }
    }
    return best;
  }

  // -----------------------------------------------------------------------
  // Rendering
  // -----------------------------------------------------------------------

  /**
   * Draws the territory as a filled polygon with a colored border.
   *
   * @param gc    GraphicsContext (already translated to world space)
   * @param color The player's dough color
   */
  public void drawTerritory(GraphicsContext gc, Color color) {
    if (polygon.size() < 3)
      return;

    double[] xs = new double[polygon.size()];
    double[] ys = new double[polygon.size()];
    for (int i = 0; i < polygon.size(); i++) {
      xs[i] = polygon.get(i).getX();
      ys[i] = polygon.get(i).getY();
    }

    gc.save();

    // Filled interior — 100% opaque, no outline
    gc.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 1.0));
    gc.fillPolygon(xs, ys, polygon.size());

    gc.restore();
  }

  // -----------------------------------------------------------------------
  // Misc
  // -----------------------------------------------------------------------

  /** Clears territory (on player death). */
  public void clearTerritory() {
    polygon.clear();
  }

  /** Returns a copy of the current polygon vertices. */
  public List<Point2D> getPolygon() {
    return new ArrayList<>(polygon);
  }

  /**
   * Rough tile-count equivalent for the HUD percentage display.
   * Uses the polygon area divided by a reference tile area.
   */
  public double getApproximateAreaFraction(double totalArenaArea) {
    if (polygon.size() < 3)
      return 0;
    return Math.abs(signedArea(polygon)) / totalArenaArea;
  }
}