package com.tesis.pickride.utils;

import android.util.Log;

import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GeoUtils {
  private final static double EARTH_RADIUS = 6371008.8;

  // Method to check if a coordinate is inside a circular geofence defined by CircleOptions
  public static boolean isCoordInsideCircle(LatLng coord, CircleOptions circleOptions) {
    // Get the center and radius of the circle
    LatLng center = circleOptions.getCenter();
    double radius = circleOptions.getRadius();  // Radius in meters

    // Calculate the distance between the center of the circle and the given coordinate
    double distance = calculateDistanceInMeters(center, coord);

    // If the distance is less than or equal to the radius, the coordinate is inside the circle
    return distance <= radius;
  }

  // Helper method to calculate the distance between two LatLng points in meters
  private static double calculateDistanceInMeters(LatLng point1, LatLng point2) {
    final int EARTH_RADIUS = 6371000; // Radius of the Earth in meters

    double lat1 = Math.toRadians(point1.latitude);
    double lon1 = Math.toRadians(point1.longitude);
    double lat2 = Math.toRadians(point2.latitude);
    double lon2 = Math.toRadians(point2.longitude);

    double deltaLat = lat2 - lat1;
    double deltaLon = lon2 - lon1;

    double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
            Math.cos(lat1) * Math.cos(lat2) *
                    Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return EARTH_RADIUS * c;  // Distance in meters
  }

  public static boolean isCoordInsidePolygon(LatLng coord, List<LatLng> polygon) {
    double x = coord.longitude;
    double y = coord.latitude;
    boolean inside = false;

    int n = polygon.size();
    for (int i = 0, j = n - 1; i < n; j = i++) {
      double xi = polygon.get(i).longitude;
      double yi = polygon.get(i).latitude;
      double xj = polygon.get(j).longitude;
      double yj = polygon.get(j).latitude;

      // Check if the ray intersects with the edge of the polygon
      boolean intersect = ((yi > y) != (yj > y)) &&
              (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
      if (intersect) {
        inside = !inside;
      }
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
