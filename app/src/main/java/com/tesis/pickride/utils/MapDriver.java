package com.tesis.pickride.utils;

import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tesis.pickride.model.DriverPoint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MapDriver {
    private GoogleMap mMap;
    private List<Marker> dynamicMarkers = new ArrayList<>();  // Ensure it's initialized

    public MapDriver(GoogleMap map) {
        this.mMap = map;
    }

    public void displayDriversOnMap(Context context) {
        if (mMap == null) {
            Toast.makeText(context, "Map is not ready yet", Toast.LENGTH_SHORT).show();
            return;  // Exit if map is not ready
        }

        List<DriverPoint> drivers = loadDrivers(context);
        if (drivers != null) {
            for (DriverPoint driver : drivers) {
                LatLng driverLocation = new LatLng(driver.getLatitude(), driver.getLongitude());
                Marker driverMarker = mMap.addMarker(new MarkerOptions()
                        .position(driverLocation)
                        .title(driver.getName())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                dynamicMarkers.add(driverMarker);
            }
        } else {
            Toast.makeText(context, "Error loading drivers", Toast.LENGTH_SHORT).show();
        }
    }

    private List<DriverPoint> loadDrivers(Context context) {
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
