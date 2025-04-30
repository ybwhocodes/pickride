package com.tesis.pickride.activity;

import static java.lang.System.currentTimeMillis;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.tesis.pickride.R;
import com.tesis.pickride.core.GeofenceTime;
import com.tesis.pickride.core.Graph;
import com.tesis.pickride.model.Route;
import com.tesis.pickride.model.RoutePoint;
import com.tesis.pickride.utils.MapDriver;
import com.tesis.pickride.utils.MapUtils;
import com.tesis.pickride.utils.MarkerClickHandler;
import com.tesis.pickride.utils.RouteCalculator;
import com.tesis.pickride.utils.RouteLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private MarkerClickHandler markerClickHandler;
    private List<Marker> dynamicMarkers = new ArrayList<>();
    private Polygon currentPolygon; // Variable to hold the current polygon
    private List<RoutePoint> polyline; // Store the polyline
    private MapDriver mapDriver; // Instance of MapDriver

    private Graph graph;
    private List<Route> routes;
    private long DijkstraTime;
    private EditText timeInput;
    private boolean runWithTBG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        timeInput = findViewById(R.id.timeInput);
        Button button1 = findViewById(R.id.button1);
        Button button2 = findViewById(R.id.button2);
        Button resetButton = findViewById(R.id.resetButton);

        button1.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, button1);
            popup.getMenuInflater().inflate(R.menu.menu_button1, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                handleButton1Selection(item, timeInput);
                return true;
            });
            popup.show();
        });

        button2.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, button2);
            popup.getMenuInflater().inflate(R.menu.menu_button2, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                handleButton2Selection(item);
                return true;
            });
            popup.show();
        });

        resetButton.setOnClickListener(v -> {
            for (Marker marker : dynamicMarkers) {
                marker.remove();
            }
            dynamicMarkers.clear();
            markerClickHandler.reset(); // Reset marker click handler
            timeInput.setText(""); // Clear the time input field

            mapDriver.resetDriver();

            // Clear the polygon if it exists
            if (currentPolygon != null) {
                currentPolygon.remove();
                currentPolygon = null;
            }
        });
    }

    @SuppressLint("NonConstantResourceId")
    private void handleButton1Selection(MenuItem item, EditText timeInput) {
        switch (item.getItemId()) {
            case R.id.calculatePoint:
                String timeStr = timeInput.getText().toString();
                if (!TextUtils.isEmpty(timeStr)) {
                    try {
                        int timeInMinutes = Integer.parseInt(timeStr);
                        LatLng startPoint = markerClickHandler.getStartPoint();
                        // Clear previous polygon if exists
                        if (currentPolygon != null) {
                            currentPolygon.remove();
                        }
                        List<LatLng> destinations = RouteCalculator.calculateDestinations(MainActivity.this, startPoint, mMap, timeInMinutes);
                        for (LatLng destination : destinations) {
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(destination)
                                    .title("Destination")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                            dynamicMarkers.add(marker);
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, "Invalid input", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please enter time in minutes", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.buildGeofence:
                timeStr = timeInput.getText().toString();
                if (!TextUtils.isEmpty(timeStr)) {
                    try {
                        int timeInMinutes = Integer.parseInt(timeStr);
                        LatLng startPoint = markerClickHandler.getStartPoint();
                        if (startPoint != null) {
                            for (Marker marker : dynamicMarkers) {
                                marker.remove();
                            }
                            dynamicMarkers.clear();
                            if (currentPolygon != null) {
                                currentPolygon.remove();
                            }

                            List<LatLng> destinations = RouteCalculator.calculateDestinations(MainActivity.this, startPoint, mMap, timeInMinutes);
                            List<LatLng> updatedDestinations = GeofenceTime.updateDestinations(MainActivity.this, startPoint, destinations, timeInMinutes, polyline);
                            if (updatedDestinations.size() != destinations.size()) {
                                Toast.makeText(MainActivity.this, "Data tidak valid", Toast.LENGTH_LONG).show();
                            }else {
                                // Add new markers
//                                Toast.makeText(MainActivity.this, "Data Update = " + updatedDestinations.size() + " Data Old =  "+ destinations.size(), Toast.LENGTH_LONG).show();
//                                for (LatLng updatedDestination : updatedDestinations) {
//                                    Marker marker = mMap.addMarker(new MarkerOptions()
//                                            .position(updatedDestination)
//                                            .title("Updated Destination")
//                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
//                                    dynamicMarkers.add(marker);
//                                }
                                sortPointsToFormPolygon(updatedDestinations);
                                drawGeofencePolygon(updatedDestinations);
                                mapDriver.setGeofence(updatedDestinations);
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Start point is not set", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, "Invalid input", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please enter time in minutes", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @SuppressLint("NonConstantResourceId")
    private void handleButton2Selection(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.route_a_star:
                try {
                    mapDriver.displayDriversOnMap(this);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error" + e, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.route_djikstra:
                this.start30x(1);
                break;
            // Handle other routes if needed
        }
    }

    private void start30x(int counter) {
        if (counter == 2) return;
        this.runDijkstra(false, counter);
    }

    private void runDijkstra(boolean filtered, int counter) {
        Thread t = new Thread(() -> {

            if (graph == null) {
                Toast.makeText(MainActivity.this, "Graph not ready", Toast.LENGTH_SHORT).show();
                return;
            }
            if (routes.isEmpty()) {
                Toast.makeText(MainActivity.this, "Routes not ready", Toast.LENGTH_SHORT).show();
                return;
            }

            long beforeRuntime = currentTimeMillis();

            List<LatLng> drivers = mapDriver.getDrivers(this, filtered);
            RoutePoint startPoint = findRoutePoint(markerClickHandler.getStartPoint());

            Map<RoutePoint, Double> shortestPaths = graph.dijkstra(startPoint);

            Log.d("Shortest", "driver: "+drivers.size());

            RoutePoint shortest = null;
            double shortestValue = 99999.99;

            for (Map.Entry<RoutePoint, Double> entry : shortestPaths.entrySet()) {
                for (LatLng driver : drivers) {
                    RoutePoint rpDriver = nearestRoutePoint(driver);
                    if (entry.getKey().getId().equals(rpDriver.getId())) {
                        if (entry.getValue() < shortestValue) {
                            shortestValue = entry.getValue();
                            shortest = entry.getKey();
                        }
                    }

                }
            }

            long afterRuntime = currentTimeMillis();

            Log.d("Shortest distance ", "to ->"+shortest.getId());

            if (!filtered) {
                this.DijkstraTime = afterRuntime - beforeRuntime;
                this.runDijkstra(true, counter);
            }
            else {
                // source, time geofence, processing time dijkstra, processing time tbg
                Log.d("collecting data", markerClickHandler.getStartPoint()+","+timeInput.getText()+","+this.DijkstraTime+","+(afterRuntime-beforeRuntime));
                this.start30x(counter+1);
            }
        });

        t.start();
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

    private RoutePoint findRoutePoint(LatLng latLng) {
        List<RoutePoint> points = RouteLoader.getAllRoutePoints();

        for (RoutePoint point : points)
            if (point.getLat() == latLng.latitude && point.getLng() == latLng.longitude)
                return point;

        return null;
    }
    private void sortPointsToFormPolygon(List<LatLng> points) {
        if (points.size() < 3) return; // Not enough points to form a polygon

        // Calculate the centroid of the points
        LatLng centroid = calculateCentroid(points);

        // Sort points by angle relative to the centroid
        Collections.sort(points, new Comparator<LatLng>() {
            @Override
            public int compare(LatLng a, LatLng b) {
                double angleA = Math.atan2(a.latitude - centroid.latitude, a.longitude - centroid.longitude);
                double angleB = Math.atan2(b.latitude - centroid.latitude, b.longitude - centroid.longitude);
                return Double.compare(angleA, angleB);
            }
        });
    }
    private LatLng calculateCentroid(List<LatLng> points) {
        double sumLat = 0;
        double sumLng = 0;

        for (LatLng point : points) {
            sumLat += point.latitude;
            sumLng += point.longitude;
        }

        return new LatLng(sumLat / points.size(), sumLng / points.size());
    }
    private void drawGeofencePolygon(List<LatLng> updatedDestinations) {
        if (currentPolygon != null) {
            currentPolygon.remove();
        }

        PolygonOptions geofencePolygonOptions = new PolygonOptions()
                .addAll(updatedDestinations)
                .strokeColor(Color.BLACK)
                .fillColor(0x550000FF);
        currentPolygon = mMap.addPolygon(geofencePolygonOptions);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng malang = new LatLng(-7.972124, 112.620497);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(malang, 14));

        int pointsCounter = 0;

        routes = RouteLoader.loadRoutes(this); // Load routes once
        for (Route route : routes) {
            pointsCounter += route.getRoutePoints().size();
            MapUtils.drawRoute(mMap, route);
            polyline = route.getRoutePoints(); // Assign the polyline from the route
        }

        markerClickHandler = new MarkerClickHandler(mMap);
        MarkerClickHandler.setInstance(markerClickHandler); // Store the instance
        mMap.setOnMapClickListener(markerClickHandler);
        mapDriver = new MapDriver(mMap);  // Initialize with the ready map

        List<RouteLoader.Interchange> interchanges = RouteLoader.loadInterchanges(this, routes);
        graph = new Graph(pointsCounter);

        for (Route route : routes) {
            List<RoutePoint> points = route.getRoutePoints();
            for (int i=0; i<points.size()-1; i++) {
                for (RouteLoader.Interchange interchange : interchanges) {
                    graph.addEdge(points.get(i), points.get(i+1));
                    graph.addEdge(points.get(i+1), points.get(i));

                    if (interchange.contains(points.get(i))) {
                        for (RoutePoint interchangePoint : interchange.getPoints()) {
                            if (!interchangePoint.getId().equals(points.get(i).getId())) {
                                graph.addEdge(points.get(i), interchangePoint);
                            }
                        }
                    }
                }
            }
        }
    }
}
