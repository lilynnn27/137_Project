package app.game_logic;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class TrailManager {
  private List<Point2D> currentTrail = new ArrayList<>();
  private final double MIN_DIST = 10.0; // Distance before adding a new point

  public void updateTrail(double x, double y, boolean isInsideTerritory) {
    if (isInsideTerritory) {
      currentTrail.clear(); // Clear trail when safe
      return;
    }

    Point2D newPoint = new Point2D(x, y);
    if (currentTrail.isEmpty() || currentTrail.get(currentTrail.size() - 1).distance(newPoint) > MIN_DIST) {
      currentTrail.add(newPoint);
    }
  }

  // Check if player hit their own trail
  public boolean checkSelfCollision(double playerX, double playerY) {
    if (currentTrail.size() < 10)
      return false; // Ignore points right behind the player

    Point2D head = new Point2D(playerX, playerY);
    for (int i = 0; i < currentTrail.size() - 10; i++) {
      if (currentTrail.get(i).distance(head) < 15) {
        return true; // Eliminated!
      }
    }
    return false;
  }

  public void drawDebugTrail(javafx.scene.canvas.GraphicsContext gc, Color color) {
    if (currentTrail.size() < 2)
      return;
    gc.setStroke(color);
    gc.setLineWidth(10);
    gc.beginPath();
    gc.moveTo(currentTrail.get(0).getX(), currentTrail.get(0).getY());
    for (Point2D p : currentTrail) {
      gc.lineTo(p.getX(), p.getY());
    }
    gc.stroke();
  }
}