package com.tesis.pickride.model;

import java.util.List;

public class PathResult {
  private double distance;  // Shortest distance
  private List<RoutePoint> path;  // Path from start to this point

  public PathResult(double distance, List<RoutePoint> path) {
    this.distance = distance;
    this.path = path;
  }

  public double getDistance() {
    return distance;
  }

  public List<RoutePoint> getPath() {
    return path;
  }
}
