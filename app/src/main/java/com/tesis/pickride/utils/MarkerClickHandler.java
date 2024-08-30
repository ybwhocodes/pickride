package com.tesis.pickride.utils;

import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.tesis.pickride.model.RoutePoint;
import com.tesis.pickride.utils.RouteLoader;

import java.util.Arrays;
import java.util.List;

public class MarkerClickHandler implements GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    private Marker userMarker;
    private Circle startPointCircle;
    private Polyline connectionLine;
    private LatLng startPoint;

    public MarkerClickHandler(GoogleMap googleMap) {
        this.mMap = googleMap;
    }
    private static MarkerClickHandler instance;

    public static MarkerClickHandler getInstance() {
        return instance;
    }

    public static void setInstance(MarkerClickHandler instance) {
        MarkerClickHandler.instance = instance;
    }
    @Override
    public void onMapClick(LatLng latLng) {
        if (userMarker != null) {
            userMarker.remove();
        }
        userMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("User Point").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        startPoint = findNearestPointOnPolyline(latLng);

        if (startPointCircle != null) {
            startPointCircle.remove();
        }
        startPointCircle = mMap.addCircle(new CircleOptions()
                .center(startPoint)
                .radius(2)
                .strokeColor(Color.BLACK)
                .fillColor(Color.BLACK));

        if (connectionLine != null) {
            connectionLine.remove();
        }
        connectionLine = mMap.addPolyline(new PolylineOptions()
                .add(latLng, startPoint)
                .color(Color.BLACK)
                .pattern(Arrays.asList(new Dot(), new Gap(20))));
        Log.d("map click", latLng+" map click");

    }

    private LatLng findNearestPointOnPolyline(LatLng userPoint) {
        List<RoutePoint> points = RouteLoader.getAllRoutePoints();
        LatLng nearestPoint = null;
        double minDistance = Double.MAX_VALUE;

        for (RoutePoint point : points) {
            LatLng routePoint = new LatLng(point.getLat(), point.getLng());
            double distance = euclideanDistance(userPoint, routePoint);
            if (distance < minDistance) {
                minDistance = distance;
                nearestPoint = routePoint;
            }
        }

        return nearestPoint;
    }

    private double euclideanDistance(LatLng start, LatLng end) {
        double latDiff = start.latitude - end.latitude;
        double lngDiff = start.longitude - end.longitude;
        return Math.sqrt(latDiff * latDiff + lngDiff * lngDiff);
    }

    public LatLng getStartPoint() {
        return startPoint;
    }

    public void reset() {
        if (userMarker != null) {
            userMarker.remove();
            userMarker = null;
        }
        if (startPointCircle != null) {
            startPointCircle.remove();
            startPointCircle = null;
        }
        if (connectionLine != null) {
            connectionLine.remove();
            connectionLine = null;
        }
        startPoint = null;
    }
}
