package com.tesis.pickride.utils;

import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.tesis.pickride.activity.MasterActivity;
import com.tesis.pickride.model.Route;
import com.tesis.pickride.model.RoutePoint;

import java.util.ArrayList;
import java.util.List;

public class RouteCalculator {

    public static List<LatLng> calculateDestinations(Context context, LatLng startPoint, GoogleMap map, int timeInMinutes) {

        if (startPoint == null) {
            Toast.makeText(context, "Start point is not set", Toast.LENGTH_SHORT).show();
            return new ArrayList<>();
        }

        List<Route> routes = RouteLoader.loadRoutes(context);
        List<LatLng> destinations = new ArrayList<>();
        double timeInHours = timeInMinutes / 60.0;
        double averageSpeed = 50;
        double radius = (((averageSpeed * timeInHours)/2) * 1000);

        PolygonOptions circlePolygonOptions = createCirclePolygon(startPoint, radius, 36);
        Polygon circlePolygon = map.addPolygon(circlePolygonOptions);

        for (Route route : routes) {
            List<LatLng> polyline = getPolyline(route);
            for (int i = 0; i < polyline.size() - 1; i++) {
                LatLng start = polyline.get(i);
                LatLng end = polyline.get(i + 1);
                List<LatLng> intersections = getLinePolygonIntersections(start, end, circlePolygon);
                for (LatLng intersection : intersections) {
                    if (!destinations.contains(intersection)) {
                        destinations.add(intersection);
                    }
                }
            }
        }

        // Store the current polygon to be used for resetting
        ((MasterActivity) context).setCurrentPolygon(circlePolygon);

        return destinations;
    }

    private static double calculateAverageSpeed(List<Route> routes) {
        double totalSpeed = 0;
        int count = 0;

        for (Route route : routes) {
            for (RoutePoint point : route.getRoutePoints()) {
                totalSpeed += getSpeedInKmPerHour(point.getSpeed());
                count++;
            }
        }

        return count > 0 ? totalSpeed / count : 0;
    }

    private static List<LatLng> getPolyline(Route route) {
        List<LatLng> polyline = new ArrayList<>();
        for (RoutePoint point : route.getRoutePoints()) {
            polyline.add(new LatLng(point.getLat(), point.getLng()));
        }
        return polyline;
    }
    public static double calculateAverageSpeedForSegment(List<RoutePoint> polyline, LatLng start, LatLng end) {
        double totalSpeed = 0;
        int count = 0;

        boolean segmentStarted = false;

        for (RoutePoint point : polyline) {
            LatLng currentLatLng = new LatLng(point.getLat(), point.getLng());

            if (currentLatLng.equals(start)) {
                segmentStarted = true;
            }

            if (segmentStarted) {
                totalSpeed += getSpeedInKmPerHour(point.getSpeed());
                count++;
            }

            if (currentLatLng.equals(end)) {
                break;
            }
        }

        return count > 0 ? totalSpeed / count : 0;
    }

    public static double calculateDistanceInMeters(RoutePoint pointA, RoutePoint pointB) {
        final int R = 6371000; // Radius of the Earth in meters
        double lat1 = Math.toRadians(pointA.getLat());
        double lat2 = Math.toRadians(pointB.getLat());
        double lng1 = Math.toRadians(pointA.getLng());
        double lng2 = Math.toRadians(pointB.getLng());

        double deltaLat = lat2 - lat1;
        double deltaLng = lng2 - lng1;

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
            Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in meters
    }

    public static double getSpeedInKmPerHour(String speed) {
        switch (speed) {
            case "Fast":
                return 50;
            case "Normal":
                return 30;
            case "Slow":
                return 10;
            default:
                return 0;
        }
    }

    public static PolygonOptions createCirclePolygon(LatLng center, double radius, int steps) {
        List<LatLng> coordinates = new ArrayList<>();

        for (int i = 0; i < steps; i++) {
            double angle = 2 * Math.PI * i / steps;
            double lat1 = degreesToRadians(center.latitude);
            double lon1 = degreesToRadians(center.longitude);

            double lat2 = Math.asin(
                    Math.sin(lat1) * Math.cos(radius / EARTH_RADIUS) +
                            Math.cos(lat1) * Math.sin(radius / EARTH_RADIUS) * Math.cos(angle)
            );

            double lon2 = lon1 + Math.atan2(
                    Math.sin(angle) * Math.sin(radius / EARTH_RADIUS) * Math.cos(lat1),
                    Math.cos(radius / EARTH_RADIUS) - Math.sin(lat1) * Math.sin(lat2)
            );

            coordinates.add(new LatLng(radiansToDegrees(lat2), radiansToDegrees(lon2)));
        }
        coordinates.add(coordinates.get(0));

        return new PolygonOptions().addAll(coordinates).strokeWidth(2).fillColor(0x550000FF).strokeColor(0x550000FF);
    }

    private static boolean isPointInPolygon(LatLng point, Polygon polygon) {
        List<LatLng> vertices = polygon.getPoints();
        int intersectCount = 0;

        for (int j = 0; j < vertices.size() - 1; j++) {
            if (rayCastIntersect(point, vertices.get(j), vertices.get(j + 1))) {
                intersectCount++;
            }
        }

        return (intersectCount % 2) == 1; // odd = inside, even = outside
    }

    private static boolean rayCastIntersect(LatLng point, LatLng vertA, LatLng vertB) {
        double aY = vertA.latitude;
        double bY = vertB.latitude;
        double aX = vertA.longitude;
        double bX = vertB.longitude;
        double pY = point.latitude;
        double pX = point.longitude;

        if ((aY > pY && bY > pY) || (aY < pY && bY < pY) || (aX < pX && bX < pX)) {
            return false;
        }

        double m = (aY - bY) / (aX - bX);
        double bee = (-aX) * m + aY;
        double x = (pY - bee) / m;

        return x > pX;
    }

    private static List<LatLng> getLinePolygonIntersections(LatLng start, LatLng end, Polygon polygon) {
        List<LatLng> intersections = new ArrayList<>();
        List<LatLng> vertices = polygon.getPoints();

        for (int i = 0; i < vertices.size() - 1; i++) {
            LatLng intersection = getLineIntersection(start, end, vertices.get(i), vertices.get(i + 1));
            if (intersection != null) {
                intersections.add(intersection);
            }
        }

        return intersections;
    }

    private static LatLng getLineIntersection(LatLng p1, LatLng p2, LatLng q1, LatLng q2) {
        double s1_x = p2.longitude - p1.longitude;
        double s1_y = p2.latitude - p1.latitude;
        double s2_x = q2.longitude - q1.longitude;
        double s2_y = q2.latitude - q1.latitude;

        double s = (-s1_y * (p1.longitude - q1.longitude) + s1_x * (p1.latitude - q1.latitude)) / (-s2_x * s1_y + s1_x * s2_y);
        double t = ( s2_x * (p1.latitude - q1.latitude) - s2_y * (p1.longitude - q1.longitude)) / (-s2_x * s1_y + s1_x * s2_y);

        if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
            double intersectionLon = p1.longitude + (t * s1_x);
            double intersectionLat = p1.latitude + (t * s1_y);
            return new LatLng(intersectionLat, intersectionLon);
        }

        return null;
    }

    private static double degreesToRadians(double degrees) {
        return degrees * Math.PI / 180;
    }

    private static double radiansToDegrees(double radians) {
        return radians * 180 / Math.PI;
    }

    public static final double EARTH_RADIUS = 6371008.8; // meters
}
