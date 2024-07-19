package com.tesis.pickride.utils;

import android.graphics.Color;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.tesis.pickride.model.Route;
import com.tesis.pickride.model.RoutePoint;

import java.util.List;

public class MapUtils {
    public static void drawRoute(GoogleMap map, Route route) {
        List<RoutePoint> points = route.getRoutePoints();
        PolylineOptions polylineOptions = new PolylineOptions();
        for (RoutePoint point : points) {
            LatLng latLng = new LatLng(point.getLat(), point.getLng());
            polylineOptions.add(latLng);
            polylineOptions.color(getColorBasedOnSpeed(point.getSpeed()));
        }
        map.addPolyline(polylineOptions);
    }

    private static int getColorBasedOnSpeed(String speed) {
        switch (speed) {
            case "Fast":
                return Color.BLUE;
            case "Normal":
                return Color.GREEN;
            case "Slow":
                return Color.RED;
            default:
                return Color.BLACK;
        }
    }
}