package com.tesis.pickride.core;

import static com.tesis.pickride.utils.RouteCalculator.EARTH_RADIUS;

import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;
import com.tesis.pickride.model.RoutePoint;
import com.tesis.pickride.utils.RouteCalculator;

import java.util.ArrayList;
import java.util.List;

public class GeofenceTime {

    public static List<LatLng> updateDestinations(Context context, LatLng startPoint, List<LatLng> destinations, int timeInMinutes, List<RoutePoint> polyline) {
        double timeInHours = timeInMinutes / 60.0;

        List<LatLng> updatedDestinations = new ArrayList<>();

        // Create a circle polygon to filter polyline points within the specified radius
        double averageSpeed = 50;
        double radius = (((averageSpeed * timeInHours)/2)*1000);; // convert to meters
        PolygonOptions circlePolygonOptions = RouteCalculator.createCirclePolygon(startPoint, radius, 36);

        for (LatLng destination : destinations) {
            LatLng newPoint = getUpdatedPoint(startPoint, destination, timeInHours, polyline, circlePolygonOptions);
            updatedDestinations.add(newPoint);
        }
        return updatedDestinations;


    }

    private static LatLng getUpdatedPoint(LatLng startPoint, LatLng destination, double timeInHours, List<RoutePoint> polyline, PolygonOptions circlePolygonOptions) {
        double remainingTime = timeInHours;
        LatLng currentPoint = startPoint;
        LatLng updatedPoint = destination;
        List<LatLng> updatedDestinations = new ArrayList<>();

        for (int i = 0; i < polyline.size() - 1; i++) {
            LatLng start = currentPoint;
            LatLng end = new LatLng(polyline.get(i + 1).getLat(), polyline.get(i + 1).getLng());

            if (isPointInPolygon(start, circlePolygonOptions) && isPointInPolygon(end, circlePolygonOptions)) {
                double segmentDistance = calculateSegmentDistance(start, end);
                double averageSpeed = RouteCalculator.calculateAverageSpeedForSegment(polyline, start, end);
                double segmentTime = segmentDistance / averageSpeed;

                // If the segment time is greater than or equal to the remaining time
                if (segmentTime <= remainingTime) {
                    double newLat = end.latitude + (end.latitude - start.latitude) ;
                    double newLng = end.longitude + (end.longitude - start.longitude) ;
                    updatedPoint = new LatLng(newLat, newLng);
                    updatedDestinations.add(updatedPoint);
                    // Return the new updated point once found
                    return updatedPoint;
                } else {
                    updatedDestinations.add(destination);
                }
            }
        }

        return destination;
    }




    private static boolean isPointInPolygon(LatLng point, PolygonOptions polygonOptions) {
        List<LatLng> vertices = polygonOptions.getPoints();
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

    private static double calculateDistance(List<RoutePoint> polyline, LatLng start, LatLng end) {
        double totalDistance = 0.0;
        LatLng previousPoint = start;

        for (RoutePoint point : polyline) {
            LatLng currentPoint = new LatLng(point.getLat(), point.getLng());

            // Calculate distance between the previous point and the current point
            double distance = calculateSegmentDistance(previousPoint, currentPoint);
            totalDistance += distance;

            previousPoint = currentPoint;
        }

        // Add distance from the last polyline point to the end point
        totalDistance += calculateSegmentDistance(previousPoint, end);

        return totalDistance;
    }

    private static double calculateSegmentDistance(LatLng start, LatLng end) {
        double lat1 = Math.toRadians(start.latitude);
        double lon1 = Math.toRadians(start.longitude);
        double lat2 = Math.toRadians(end.latitude);
        double lon2 = Math.toRadians(end.longitude);

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = RouteCalculator.EARTH_RADIUS * c;

        return distance;
    }

    private static double calculateAverageSpeed(List<RoutePoint> polyline) {
        double totalSpeed = 0;
        int count = 0;

        for (RoutePoint point : polyline) {
            totalSpeed += RouteCalculator.getSpeedInKmPerHour(point.getSpeed());
            count++;
        }

        return count > 0 ? totalSpeed / count : 0;
    }
    public static PolygonOptions createGeofencePolygon(List<LatLng> updatedDestinations) {
        PolygonOptions geofencePolygon = new PolygonOptions();
        for (LatLng point : updatedDestinations) {
            geofencePolygon.add(point);
        }
        return geofencePolygon;
    }
}
