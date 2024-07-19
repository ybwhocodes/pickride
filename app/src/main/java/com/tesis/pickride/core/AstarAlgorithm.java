package com.tesis.pickride.core;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.tesis.pickride.activity.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class AstarAlgorithm {
    private Polygon currentPolygon;
    private List<LatLng> polygonPoints;

    // Constructor to initialize with the current polygon
    public AstarAlgorithm(MainActivity mainActivity) {
        this.currentPolygon = mainActivity.getCurrentPolygon();
        if (this.currentPolygon != null) {
            this.polygonPoints = this.currentPolygon.getPoints();
        } else {
            this.polygonPoints = new ArrayList<>();
        }
    }

    // Example method to demonstrate using the polygon points
    public void processPolygon() {
        if (polygonPoints != null && !polygonPoints.isEmpty()) {
            // Implement A* algorithm logic here using polygonPoints
            for (LatLng point : polygonPoints) {
                // Example processing
                System.out.println("Point: " + point.latitude + ", " + point.longitude);
            }
        } else {
            System.out.println("No polygon points available");
        }
    }
}
