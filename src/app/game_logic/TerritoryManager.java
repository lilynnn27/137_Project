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

  // ---- Bounding-box cache for fast point-rejection ----
  private double bbMinX = 0, bbMaxX = 0, bbMinY = 0, bbMaxY = 0;
  private boolean bbDirty = true;

  private void rebuildBoundingBox() {
    bbMinX = Double.MAX_VALUE; bbMaxX = -Double.MAX_VALUE;
    bbMinY = Double.MAX_VALUE; bbMaxY = -Double.MAX_VALUE;
    for (Point2D p : polygon) {
      if (p.getX() < bbMinX) bbMinX = p.getX();
      if (p.getX() > bbMaxX) bbMaxX = p.getX();
      if (p.getY() < bbMinY) bbMinY = p.getY();
      if (p.getY() > bbMaxY) bbMaxY = p.getY();
    }
    bbDirty = false;
  }

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
    bbDirty = true;
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

    // Fast bounding-box rejection (avoids O(N) loop most of the time)
    if (bbDirty) rebuildBoundingBox();
    if (px < bbMinX || px > bbMaxX || py < bbMinY || py > bbMaxY)
      return false;

    int n = polygon.size();

    // Points within this distance of any edge are treated as inside,
    // preventing ray-cast flicker when the player is exactly on the boundary.
    final double BOUNDARY_EPSILON = 2.0;
    for (int i = 0, j = n - 1; i < n; j = i++) {
      if (segmentDistance(px, py, polygon.get(j), polygon.get(i)) < BOUNDARY_EPSILON)
        return true;
    }

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

    StitchPoint exit  = nearestEdgeProjection(trailStart.getX(), trailStart.getY());
    StitchPoint entry = nearestEdgeProjection(trailEnd.getX(),   trailEnd.getY());

    double currentArea = Math.abs(signedArea(polygon));

    // Candidate A: walk boundary exit→entry (one arc), then trail REVERSED
    // (entry→exit).
    // The reversal is required so the polygon closes correctly without
    // self-intersection.
    List<Point2D> candA = buildCandidatePolygon(exit,  entry, trail, true);

    // Candidate B: walk boundary entry→exit (the other arc), then trail FORWARD
    // (exit→entry).
    List<Point2D> candB = buildCandidatePolygon(entry, exit,  trail, false);

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
      bestArea = areaB;
    }

    polygon = best;
    bbDirty = true; // polygon changed — invalidate bounding box
  }

  /** Build one candidate polygon by stitching a boundary arc + trail. */
  private List<Point2D> buildCandidatePolygon(StitchPoint from, StitchPoint to,
      List<Point2D> trail, boolean reverseTrail) {
    int n = polygon.size();
    List<Point2D> result = new ArrayList<>();

    // Start at the exact projected crossing point on the boundary
    result.add(from.proj());

    // Walk interior boundary vertices from the edge after 'from' up to 'to's edge
    int i = (from.edgeIdx() + 1) % n;
    while (true) {
      result.add(polygon.get(i));
      if (i == to.edgeIdx()) break;
      i = (i + 1) % n;
    }

    // End at the exact projected crossing point for the other endpoint
    result.add(to.proj());

    // Append the trail
    if (reverseTrail) {
      for (int t = trail.size() - 1; t >= 0; t--)
        result.add(trail.get(t));
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

  private static final class StitchPoint {
    private final int edgeIdx;
    private final Point2D proj;

    StitchPoint(int edgeIdx, Point2D proj) {
      this.edgeIdx = edgeIdx;
      this.proj    = proj;
    }

    int edgeIdx() { return edgeIdx; }
    Point2D proj() { return proj; }
  }

  /** Finds the nearest point on the polygon boundary via perpendicular edge projection. */
  private StitchPoint nearestEdgeProjection(double px, double py) {
    int n = polygon.size();
    double bestDist = Double.MAX_VALUE;
    int bestEdge = 0;
    Point2D bestPt = polygon.get(0);

    for (int i = 0; i < n; i++) {
      Point2D a = polygon.get(i);
      Point2D b = polygon.get((i + 1) % n);
      double dx = b.getX() - a.getX(), dy = b.getY() - a.getY();
      double lenSq = dx * dx + dy * dy;
      double t = (lenSq == 0) ? 0 :
          Math.max(0, Math.min(1,
              ((px - a.getX()) * dx + (py - a.getY()) * dy) / lenSq));
      Point2D proj = new Point2D(a.getX() + t * dx, a.getY() + t * dy);
      double dist = Math.hypot(px - proj.getX(), py - proj.getY());
      if (dist < bestDist) {
        bestDist = dist;
        bestEdge = i;
        bestPt = proj;
      }
    }

    return new StitchPoint(bestEdge, bestPt);
  }

  /** Distance from point (px, py) to segment a→b. */
  private static double segmentDistance(double px, double py, Point2D a, Point2D b) {
    double dx = b.getX() - a.getX(), dy = b.getY() - a.getY();
    double lenSq = dx * dx + dy * dy;
    if (lenSq == 0) return Math.hypot(px - a.getX(), py - a.getY());
    double t = Math.max(0, Math.min(1,
        ((px - a.getX()) * dx + (py - a.getY()) * dy) / lenSq));
    return Math.hypot(px - (a.getX() + t * dx), py - (a.getY() + t * dy));
  }

  // -----------------------------------------------------------------------
  // Rendering
  // -----------------------------------------------------------------------

  private double[] cachedXs = new double[0];
  private double[] cachedYs = new double[0];

  /**
   * Draws the territory as a filled polygon with a colored border.
   *
   * @param gc    GraphicsContext (already translated to world space)
   * @param color The player's dough color
   */
  public void drawTerritory(GraphicsContext gc, Color color) {
    if (polygon.size() < 3)
      return;

    int size = polygon.size();
    if (bbDirty || cachedXs.length != size) {
      cachedXs = new double[size];
      cachedYs = new double[size];
      for (int i = 0; i < size; i++) {
        cachedXs[i] = polygon.get(i).getX();
        cachedYs[i] = polygon.get(i).getY();
      }
    }

    gc.save();

    // Filled interior — 100% opaque, no outline
    gc.setFill(color);
    gc.fillPolygon(cachedXs, cachedYs, size);

    gc.restore();
  }

  // -----------------------------------------------------------------------
  // Misc
  // -----------------------------------------------------------------------

  /** Clears territory (on player death). */
  public void clearTerritory() {
    polygon.clear();
    bbDirty = true;
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