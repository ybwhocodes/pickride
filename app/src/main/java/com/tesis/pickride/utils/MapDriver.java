package com.tesis.pickride.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tesis.pickride.model.DriverPoint;
import com.tesis.pickride.model.RoutePoint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MapDriver {
    private GoogleMap mMap;
    private List<Marker> dynamicMarkers = new ArrayList<>();  // Ensure it's initialized
    private List<LatLng> geofence;

    public MapDriver(GoogleMap map) {
        this.mMap = map;
        geofence = new ArrayList<>();
    }

    public void setGeofence(List<LatLng> points) {
        geofence = points;
    }

    public void resetDriver() {
        for (Marker marker : dynamicMarkers) {
            marker.remove();
        }

        dynamicMarkers.clear();
        geofence.clear();
    }

    public List<LatLng> getDrivers(Context context, boolean filtered) {
        List<LatLng> drivers_tbg = new ArrayList<>();


        if (geofence.isEmpty()) {
            Toast.makeText(context, "Geofence is not ready yet", Toast.LENGTH_SHORT).show();
            return drivers_tbg;
        }

        List<DriverPoint> drivers_raw = loadDrivers(context);

        for (DriverPoint driver : drivers_raw) {
            LatLng dpoint = new LatLng(driver.getLatitude(), driver.getLongitude());
            if (filtered && !GeoUtils.isCoordInsidePolygon(dpoint, geofence)) continue;
            drivers_tbg.add(dpoint);
        }

        return drivers_tbg;
    }

    public void displayDriversOnMap(Context context) {
        if (mMap == null) {
            Toast.makeText(context, "Map is not ready yet", Toast.LENGTH_SHORT).show();
            return;  // Exit if map is not ready
        }

        if (geofence.isEmpty()) {
            Toast.makeText(context, "Geofence is not ready yet", Toast.LENGTH_SHORT).show();
        }

        List<LatLng> drivers = getDrivers(context, true);

        Log.d("Total-Driver", "driver: "+drivers.size());

        if (drivers != null) {
            for (LatLng driver : drivers) {
                Marker driverMarker = mMap.addMarker(new MarkerOptions()
                        .position(driver)
                        .title(nearestRoutePoint(driver).getId())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                dynamicMarkers.add(driverMarker);
            }
        } else {
            Toast.makeText(context, "Error loading drivers", Toast.LENGTH_SHORT).show();
        }
    }

    private RoutePoint nearestRoutePoint(LatLng latLng) {
        List<RoutePoint> points = RouteLoader.getAllRoutePoints();
        RoutePoint nearestPoint = null;
        double minDistance = Double.MAX_VALUE;

        for (RoutePoint point : points) {
            LatLng routePoint = new LatLng(point.getLat(), point.getLng());
            double distance = euclideanDistance(latLng, routePoint);
            if (distance < minDistance) {
                minDistance = distance;
                nearestPoint = point;
            }
        }

        return nearestPoint;
    }

    private double euclideanDistance(LatLng start, LatLng end) {
        double latDiff = start.latitude - end.latitude;
        double lngDiff = start.longitude - end.longitude;
        return Math.sqrt(latDiff * latDiff + lngDiff * lngDiff);
    }

    public static List<DriverPoint> loadDrivers(Context context) {
        List<DriverPoint> drivers = new ArrayList<>();
        try {
            String jsonStr = loadJSONFromAsset(context, "driver.json");
            JSONArray rootArray = new JSONArray(jsonStr);
            JSONObject rootObject = rootArray.getJSONObject(0);
            JSONArray driversArr = rootObject.getJSONArray("drivers");

            for (int i = 0; i < driversArr.length(); i++) {
                JSONObject obj = driversArr.getJSONObject(i);
                DriverPoint driver = new DriverPoint(
                        obj.getString("name"),
                        obj.getJSONObject("coordinates").getDouble("lat"),
                        obj.getJSONObject("coordinates").getDouble("lng")
                );
                drivers.add(driver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return drivers;
    }

    private static String loadJSONFromAsset(Context context, String fileName) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

}
