package com.tesis.pickride.utils;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GeoUtils {
  private final static double EARTH_RADIUS = 6371008.8;

  public static boolean isCoordInsidePolygon(LatLng coord, List<LatLng> polygon) {
    double x = coord.longitude;
    double y = coord.latitude;
    boolean inside = false;

    for (int i = 0, j = 1; j < polygon.size(); i = j++) {
      double xi = polygon.get(i).longitude;
      double yi = polygon.get(i).latitude;

      double xj = polygon.get(j).longitude;
      double yj = polygon.get(j).latitude;

      boolean intersect = ((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
      if (intersect) inside = !inside;
    }

    return inside;
  }

  public static List<LatLng> createCirclePolygon(LatLng center) {
    int CIRCLE_STEPS = 12;
    int DISTANCE = 15;

    List<LatLng> coordinates = new ArrayList<>();

    for (int i = 0; i < CIRCLE_STEPS; i++) {
      double angle = 2 * Math.PI * i / CIRCLE_STEPS;
      double lat1 = degreesToRadians(center.latitude);
      double lon1 = degreesToRadians(center.longitude);

      double lat2 = Math.asin(
          Math.sin(lat1) * Math.cos(DISTANCE / EARTH_RADIUS) +
              Math.cos(lat1) * Math.sin(DISTANCE / EARTH_RADIUS) * Math.cos(angle)
      );

      double lon2 = lon1 + Math.atan2(
          Math.sin(angle) * Math.sin(DISTANCE / EARTH_RADIUS) * Math.cos(lat1),
          Math.cos(DISTANCE / EARTH_RADIUS) - Math.sin(lat1) * Math.sin(lat2)
      );

      coordinates.add(new LatLng(radiansToDegrees(lat2), radiansToDegrees(lon2)));
    }

    // last coord == first coord, to close the polygon
    coordinates.add(coordinates.get(0));

    Log.d("circle", coordinates.toString());

    return coordinates;
  }

  private static double degreesToRadians(double degrees) {
    return degrees * Math.PI / 180;
  }

  private static double radiansToDegrees(double radians) {
    return radians * 180 / Math.PI;
  }

  private static double distanceToRadians(double distance) {
    return distance / EARTH_RADIUS;
  }
}
