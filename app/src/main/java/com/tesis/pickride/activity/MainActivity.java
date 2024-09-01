package com.tesis.pickride.activity;

import static com.tesis.pickride.utils.GeoUtils.isCoordInsideCircle;
import static java.lang.System.currentTimeMillis;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.tesis.pickride.R;
import com.tesis.pickride.core.GeofenceTime;
import com.tesis.pickride.core.Graph;
import com.tesis.pickride.model.DriverPoint;
import com.tesis.pickride.model.Route;
import com.tesis.pickride.model.RoutePoint;
import com.tesis.pickride.utils.GeoUtils;
import com.tesis.pickride.utils.MapDriver;
import com.tesis.pickride.utils.MapUtils;
import com.tesis.pickride.utils.MarkerClickHandler;
import com.tesis.pickride.utils.RouteCalculator;
import com.tesis.pickride.utils.RouteLoader;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private MarkerClickHandler markerClickHandler;
    private List<Marker> dynamicMarkers = new ArrayList<>();
    private Polygon currentPolygon;

    private Circle currentCircle;
    private List<RoutePoint> polyline; // Store the polyline
    private MapDriver mapDriver; // Instance of MapDriver

    private Graph graph;
    private List<Route> routes;
    private long DijkstraTime;
    private EditText timeInput;
    private boolean runWithTBG;
    private List<Marker> driverMarkers = new ArrayList<>();
    List<LatLng> drivers_circle = new ArrayList<>();


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
            // Remove the geofence (circle) if it exists
            if (currentCircle != null) {
                currentCircle.remove();
                currentCircle = null;
            }

            for (Marker marker : driverMarkers) {
                marker.remove();  // Remove the marker from the map
            }
            // Clear the list after all markers are removed
            driverMarkers.clear();
            drivers_circle.clear();
        });
    }

    // Method to set a circular geofence with the calculated radius
    // Method to set a circular geofence with the calculated radius and filter drivers within it
    private void setCircularGeofence(LatLng center, int timeInMinutes, GoogleMap map, Context context) {
        // Convert time in minutes to hours
        double timeInHours = timeInMinutes / 60.0;

        // Set the average speed in km/h
        double averageSpeed = 50;

        // Calculate the radius in meters (half distance traveled at average speed)
        double radius = (((averageSpeed * timeInHours) / 2) * 1000);

        // Check if there is an existing circle and remove it
        if (currentCircle != null) {
            currentCircle.remove();
        }

        // Create a circular geofence using Google Maps API
        CircleOptions circleOptions = new CircleOptions()
                .center(center)  // Set center of the geofence
                .radius(radius)  // Radius in meters
                .strokeColor(Color.RED)  // Circle border color
                .fillColor(0x30FF0000)  // Circle fill color with transparency
                .strokeWidth(2);  // Border width

        // Add the circular geofence to the map and save reference
        currentCircle = map.addCircle(circleOptions);

        // Inform the user of the geofence radius set
        Toast.makeText(this, "Geofence set with radius: " + radius + " meters", Toast.LENGTH_SHORT).show();

        // Load drivers and filter them within the geofence
        List<DriverPoint> drivers_raw = MapDriver.loadDrivers(this);  // Load drivers

        // Filter drivers within the geofence
        for (DriverPoint driver : drivers_raw) {
            LatLng dpoint = new LatLng(driver.getLatitude(), driver.getLongitude());

            // Check if the driver's location is inside the circular geofence
            if (isCoordInsideCircle(dpoint, circleOptions)) {
                drivers_circle.add(dpoint);
            }
        }
        Log.d("Total-Driver", "driver: "+drivers_circle.size());

        // Optionally, do something with the filtered list of drivers (e.g., display them on the map)
        displayDriversOnMapCircular(drivers_circle, map);
    }

    // Example method to display drivers on the map (You can adjust it as needed)
    private void displayDriversOnMapCircular(List<LatLng> drivers, GoogleMap map) {
        for (LatLng driver : drivers) {
            Marker marker = map.addMarker(new MarkerOptions().position(driver).title("Driver"));
            if (marker != null) {
                driverMarkers.add(marker);  // Keep track of the added marker
            }
        }
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

                        // Clear previous circle if exists and set a new circular geofence
                        setCircularGeofence(startPoint, timeInMinutes, mMap,this);  // Assuming 'googleMap' is your instance of GoogleMap

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

                            List<LatLng> destinations = RouteCalculator.calculateDestinations(MainActivity.this, mMap, timeInMinutes);
                            List<LatLng> updatedDestinations = GeofenceTime.updateDestinations(MainActivity.this, startPoint, destinations, timeInMinutes, polyline);
                            if (updatedDestinations.size() != destinations.size()) {
                                Toast.makeText(MainActivity.this, "Data tidak valid", Toast.LENGTH_LONG).show();
                            }else {
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

                try {
                    mapDriver.displayDriversOnMap(this);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error" + e, Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @SuppressLint("NonConstantResourceId")
    private void handleButton2Selection(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.route_a_star:

                break;
            case R.id.route_djikstra:
                this.start30x(1);
                break;
            // Handle other routes if needed
        }
    }

    private void start30x(int counter) {
        if (counter == 11) return;

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

    public void setCurrentPolygon(Polygon polygon) {
        this.currentPolygon = polygon;
    }
    public Polygon getCurrentPolygon() {
        return currentPolygon;
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
