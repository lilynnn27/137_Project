package app.game_logic;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.HashSet;
import java.util.Set;

public class TerritoryManager {
  // Stores the center coordinates of all hexes you own as a string "X,Y"
  private Set<String> ownedTiles = new HashSet<>();
  private double hexRadius = 27.5;
  private double hexW = Math.sqrt(3) * hexRadius;
  private double hexH = 2 * hexRadius;

  // Call this to "claim" the specific tile the player is standing on
  public void claimTile(double x, double y) {
    Point2D gridCenter = snapToGrid(x, y);
    String tileKey = Math.round(gridCenter.getX()) + "," + Math.round(gridCenter.getY());
    ownedTiles.add(tileKey);
  }

  public int getOwnedHexCount() {
    return ownedTiles.size();
  }

  // Draws filled hexagons for everything you own
  public void drawTerritory(GraphicsContext gc, Color playerColor) {
    gc.setFill(playerColor);
    for (String key : ownedTiles) {
      String[] parts = key.split(",");
      double centerX = Double.parseDouble(parts[0]);
      double centerY = Double.parseDouble(parts[1]);

      double[] xPoints = new double[6];
      double[] yPoints = new double[6];
      for (int i = 0; i < 6; i++) {
        double angle_rad = Math.PI / 180 * (60 * i - 30);
        xPoints[i] = centerX + hexRadius * Math.cos(angle_rad);
        yPoints[i] = centerY + hexRadius * Math.sin(angle_rad);
      }
      gc.fillPolygon(xPoints, yPoints, 6);
    }
  }

  // Helper to align raw coordinates to your specific hex grid math
  private Point2D snapToGrid(double rawX, double rawY) {
    int row = (int) (rawY / (hexH * 0.75));
    double y = row * (hexH * 0.75);
    double xOffset = (row % 2 == 0) ? 0 : hexW / 2;
    double x = Math.round((rawX - xOffset) / hexW) * hexW + xOffset;
    return new Point2D(x, y);
  }

  // Clears all territory when the player is eliminated
  public void clearTerritory() {
    ownedTiles.clear();
  }
}