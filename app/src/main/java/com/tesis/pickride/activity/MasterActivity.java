package com.tesis.pickride.activity;

import static com.tesis.pickride.utils.RouteLoader.getRandomRoutePoint;
import static java.lang.System.currentTimeMillis;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.app.ProgressDialog;

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
import com.google.android.gms.maps.model.PolylineOptions;
import com.tesis.pickride.R;
import com.tesis.pickride.core.AstarAlgorithm;
import com.tesis.pickride.core.AstarAlgorithm.HeuristicType;
import com.tesis.pickride.core.DjikstraAlgorithm;
import com.tesis.pickride.core.GeofenceTime;
import com.tesis.pickride.core.Graph;
import com.tesis.pickride.model.Route;
import com.tesis.pickride.model.RoutePoint;
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
import java.util.PriorityQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MasterActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private MarkerClickHandler markerClickHandler;
    private List<Marker> dynamicMarkers = new ArrayList<>();
    private Polygon currentPolygon; // Variable to hold the current polygon
    private List<RoutePoint> polyline; // Store the polyline
    private MapDriver mapDriver; // Instance of MapDriver
    Context context;
    private Graph graph;
    private List<Route> routes;
    private EditText timeInput;
    ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        timeInput = findViewById(R.id.input_time);
        Button button = findViewById(R.id.button3);
        Button resetButton = findViewById(R.id.resetButton);

//        button.setOnClickListener(v -> {
//            PopupMenu popup = new PopupMenu(MasterActivity.this, button);
//            popup.getMenuInflater().inflate(R.menu.menu_button3, popup.getMenu());
//            popup.setOnMenuItemClickListener(item -> {
//                handleButtonSelection(item, timeInput);
//                return true;
//            });
//            popup.show();
//        });

        button.setOnClickListener(v -> {
            LatLng startLatLng = markerClickHandler.getStartPoint();
//            Log.d("CLik-Point", "lat: "+ startLatLng.latitude+ ", lng: "+ startLatLng.longitude);
            handleButtonSelectionMultiple(timeInput, startLatLng);
        });

        resetButton.setOnClickListener(v -> {
            // Hapus semua marker
            for (Marker marker : dynamicMarkers) {
                marker.remove();
            }
            dynamicMarkers.clear();

            // Reset marker handler & input
            markerClickHandler.reset();
            timeInput.setText("");

            // Reset driver state & polygon
//            mapDriver.resetDriver();
            if (currentPolygon != null) {
                currentPolygon.remove();
                currentPolygon = null;
            }

            // Clear & tampilkan ulang semua route dari awal
            if (mMap != null) {
                mMap.clear();
                for (Route route : routes) {
                    MapUtils.drawRoute(mMap, route);
                }
            }

            // Reset polyline (jika perlu)
            int pointsCounter = 0;
            polyline = null;
            routes = RouteLoader.loadRoutes(this); // Load routes once
            for (Route route : routes) {
                pointsCounter += route.getRoutePoints().size();
                MapUtils.drawRoute(mMap, route);
                polyline = route.getRoutePoints(); // Assign the polyline from the route
            }
            mapDriver.displayAllDriversOnMap(this);
        });

    }

    @SuppressLint("NonConstantResourceId")
    private void handleButtonSelectionMultiple(EditText timeInput, LatLng startLatLng) {

        Map<String, AstarAlgorithm.Node> nodeMap = graph.toNodeMap();
        String timeStr = timeInput.getText().toString();
        RoutePoint randomPoint = getRandomRoutePoint(context);

        if (startLatLng == null) {
            Toast.makeText(this, "Titik awal belum dipilih", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TextUtils.isEmpty(timeStr)) {
            try {
                if (startLatLng != null) {
                    int timeInMinutes = Integer.parseInt(timeStr);
                    Log.d("Proses-Button", "On Prosses Algoritm");
                    Handler handler = new Handler(Looper.getMainLooper());

                    System.gc();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}
//                    runDijkstraPathfindingSingle(timeInMinutes, startLatLng);
                    runMultipleDijkstraPathfinding (timeInMinutes, startLatLng);
                    // Jeda 1 detik untuk A* MANHATTAN
                    handler.postDelayed(() -> {
                        runMultipleAstarPathfinding(timeInMinutes, startLatLng, HeuristicType.MANHATTAN);
                    }, 4000);
                    System.gc();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {}
//                    // Jeda 2 detik untuk A* EUCLIDEAN
                    handler.postDelayed(() -> {
                        runMultipleAstarPathfinding(timeInMinutes, startLatLng, HeuristicType.EUCLIDEAN);
                    }, 8000);
                    System.gc();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}
//                    // Jeda 3 detik untuk A* HAVERSINE
                    handler.postDelayed(() -> {
                        runMultipleAstarPathfinding(timeInMinutes, startLatLng, HeuristicType.HAVERSINE);
                    }, 12000);
                    System.gc();
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ignored) {}
                } else {
                    Toast.makeText(this, "No points available. ", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please enter time in minutes", Toast.LENGTH_SHORT).show();
        }

        if (graph == null || routes == null || routes.isEmpty()) {
            Toast.makeText(this, "Graph or routes not ready", Toast.LENGTH_SHORT).show();
        }

    }

    @SuppressLint("NonConstantResourceId")
    private void handleButtonSelection(MenuItem item, EditText timeInput) {

        Map<String, AstarAlgorithm.Node> nodeMap = graph.toNodeMap();
        String timeStr = timeInput.getText().toString();
        RoutePoint randomPoint = getRandomRoutePoint(context);
        LatLng startLatLng = markerClickHandler.getStartPoint();


        switch (item.getItemId()) {
            case R.id.tbg_djikstra:
                if (!TextUtils.isEmpty(timeStr)) {
                    try {
                        if (randomPoint != null) {
                            int timeInMinutes = Integer.parseInt(timeStr);
                            runMultipleDijkstraPathfinding(timeInMinutes, startLatLng);
//                            runDijkstraPathfindingSingle(timeInMinutes, startLatLng);
                        } else {
                            Toast.makeText(this, "No points available. ", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Please enter time in minutes", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.tbg_astar_md:
                // A-Star with Manhattan Distance
                if (!TextUtils.isEmpty(timeStr)) {
                    try {
                        int timeInMinutes = Integer.parseInt(timeStr);
                        runMultipleAstarPathfinding(timeInMinutes, startLatLng, AstarAlgorithm.HeuristicType.MANHATTAN);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Please enter time in minutes", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.tbg_astar_ed:
                // A-Star with Euclidean Distance

                if (!TextUtils.isEmpty(timeStr)) {
                    try {
                        int timeInMinutes = Integer.parseInt(timeStr);
                        runMultipleAstarPathfinding(timeInMinutes, startLatLng, HeuristicType.EUCLIDEAN);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Please enter time in minutes", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.tbg_astar_hf:
                // A-Star with Haversine Formula
                if (!TextUtils.isEmpty(timeStr)) {
                    try {
                        int timeInMinutes = Integer.parseInt(timeStr);
                        runMultipleAstarPathfinding(timeInMinutes, startLatLng, HeuristicType.HAVERSINE);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Please enter time in minutes", Toast.LENGTH_SHORT).show();
                }
                break;

        }

        if (graph == null || routes == null || routes.isEmpty()) {
            Toast.makeText(this, "Graph or routes not ready", Toast.LENGTH_SHORT).show();
        }

    }

    private void runAStarPathfindingSingle(int timeInMinutes, LatLng startLatLng, AstarAlgorithm.HeuristicType heuristicType) {
        long startTime = currentTimeMillis();

        if (startLatLng == null) {
            Toast.makeText(this, "Titik awal belum dipilih", Toast.LENGTH_SHORT).show();
            return;
        }

        if (polyline == null || polyline.isEmpty()) {
            Toast.makeText(this, "Polyline tidak tersedia, reset ulang aplikasi", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Marker marker : dynamicMarkers) {
            marker.remove();
        }
        dynamicMarkers.clear();

        if (currentPolygon != null) {
            currentPolygon.remove();
            currentPolygon = null;
        }

        List<LatLng> destinations = RouteCalculator.calculateDestinations(this, startLatLng, mMap, timeInMinutes);
        List<LatLng> updatedDestinations = GeofenceTime.updateDestinations(this, startLatLng, destinations, timeInMinutes, polyline);

        if (updatedDestinations == null || destinations == null) {
            Toast.makeText(this, "Destinasi gagal dihitung. Coba ulangi", Toast.LENGTH_LONG).show();
            return;
        }
        if (updatedDestinations.size() != destinations.size()) {
            Toast.makeText(this, "Data tidak valid", Toast.LENGTH_LONG).show();
            return;
        }

        sortPointsToFormPolygon(updatedDestinations);
        drawGeofencePolygon(updatedDestinations);
        mapDriver.setGeofence(updatedDestinations);

        RoutePoint start = findRoutePoint(startLatLng);
        Map<String, AstarAlgorithm.Node> nodeMap = graph.toNodeMap();
        AstarAlgorithm.Node startNode = nodeMap.get(start.getId());

        List<LatLng> drivers = mapDriver.getDrivers(this, false);
        List<LatLng> fastestPath = new ArrayList<>();
        double shortestTime = Double.MAX_VALUE;
        LatLng fastestDriver = null;

        for (LatLng driverLoc : drivers) {
            RoutePoint rpDriver = nearestRoutePoint(driverLoc);
            AstarAlgorithm.Node goalNode = nodeMap.get(rpDriver.getId());
            if (goalNode == null) continue;

            List<LatLng> path = AstarAlgorithm.findPath(nodeMap, startNode, goalNode, heuristicType);
            double pathLength = pathLengthInMinutes(path);

            if (!path.isEmpty() && pathLength < shortestTime) {
                shortestTime = pathLength;
                fastestPath = path;
                fastestDriver = driverLoc;
            }
        }

        long endTime = currentTimeMillis();
        Log.d("Time-Execution", "AStar-" + heuristicType.name() + ": " + (endTime - startTime) + "ms");

        if (!fastestPath.isEmpty()) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(fastestPath.get(0)).title("Start" + startLatLng));
            mMap.addMarker(new MarkerOptions().position(fastestPath.get(fastestPath.size() - 1)).title("Fastest Driver" + fastestDriver.toString()));
            mMap.addPolyline(new PolylineOptions().addAll(fastestPath));
            Log.d("rute_jalan", "Test" + fastestPath);
            Log.d("AStar-" + heuristicType.name() + "-FastestDriver", fastestDriver.toString());
            Log.d("AStar-" + heuristicType.name() + "-FastestTime", shortestTime + " (approx. units)");
        }

        Toast.makeText(this, "A-Star " + heuristicType.name() + " processing complete", Toast.LENGTH_SHORT).show();
    }

    private void runDijkstraPathfindingSingle(int timeInMinutes, LatLng startLatLng) {
        Runtime runtime = Runtime.getRuntime();

        long startTime = currentTimeMillis();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        if (startLatLng == null) {
            Toast.makeText(this, "Titik awal belum dipilih", Toast.LENGTH_SHORT).show();
            return;
        }

        if (polyline == null || polyline.isEmpty()) {
            Toast.makeText(this, "Polyline tidak tersedia, reset ulang aplikasi", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Marker marker : dynamicMarkers) {
            marker.remove();
        }
        dynamicMarkers.clear();

        if (currentPolygon != null) {
            currentPolygon.remove();
            currentPolygon = null;
        }

        List<LatLng> destinations = RouteCalculator.calculateDestinations(this, startLatLng, mMap, timeInMinutes);
        List<LatLng> updatedDestinations = GeofenceTime.updateDestinations(this, startLatLng, destinations, timeInMinutes, polyline);

        if (updatedDestinations == null || destinations == null) {
            Toast.makeText(this, "Destinasi gagal dihitung. Coba ulangi", Toast.LENGTH_LONG).show();
            return;
        }
        if (updatedDestinations.size() != destinations.size()) {
            Toast.makeText(this, "Data tidak valid", Toast.LENGTH_LONG).show();
            return;
        }

        sortPointsToFormPolygon(updatedDestinations);
        drawGeofencePolygon(updatedDestinations);
        mapDriver.setGeofence(updatedDestinations);

        RoutePoint start = findRoutePoint(startLatLng);
        Map<String, DjikstraAlgorithm.Node> nodeMap = graph.toDijkstraNodeMap();
        DjikstraAlgorithm.Node startNode = nodeMap.get(start.getId());

        List<LatLng> drivers = mapDriver.getDrivers(this, false);
        List<LatLng> fastestPath = new ArrayList<>();
        double shortestTime = Double.MAX_VALUE;
        LatLng fastestDriver = null;

        double shortestDistance = Double.MAX_VALUE;
        LatLng nearestDriver = null;

        for (LatLng driverLoc : drivers) {
            RoutePoint rpDriver = nearestRoutePoint(driverLoc);
            DjikstraAlgorithm.Node goalNode = nodeMap.get(rpDriver.getId());
            if (goalNode == null) continue;

            List<LatLng> path = DjikstraAlgorithm.findPath(nodeMap, startNode, goalNode);
            double pathLength = pathLengthInMinutes(path);

            if (!path.isEmpty() && pathLength < shortestTime) {
                shortestTime = pathLength;
                fastestPath = path;
                fastestDriver = driverLoc;
            }
        }
        long endTime = currentTimeMillis();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

        long memoryUsed = memoryAfter - memoryBefore;
        long timeElapsed = endTime - startTime;

        if (!fastestPath.isEmpty()) {
            double distanceToFastest = distanceInMeters(startLatLng, fastestDriver);
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(fastestPath.get(0)).title("Start" + startLatLng));
            mMap.addMarker(new MarkerOptions().position(fastestPath.get(fastestPath.size() - 1)).title("Fastest Driver" + fastestPath.get(fastestPath.size() - 1)));
            mMap.addMarker(new MarkerOptions().position(fastestDriver));
            mMap.addPolyline(new com.google.android.gms.maps.model.PolylineOptions().addAll(fastestPath));
            Log.e("Proses-Dijkstra-FastestDriver",
                    "Start: " + fastestPath.get(0) +
                            " | End: " + fastestPath.get(fastestPath.size() - 1) +
                            " | Fastest Driver: " + fastestDriver +
                            " | Waktu (menit): " + shortestTime +
                            " | Jarak (meter): " + distanceToFastest);
        }

        if (nearestDriver != null) {
            Log.e("Proses-Dijkstra-NearestDriver",
                    "Nearest Driver (jarak langsung): " + nearestDriver +
                            " | Jarak: " + shortestDistance + " meter");
        }

        Log.d("Dijkstra-Proses", "Time : " + timeElapsed + "ms, Memory :" + memoryUsed + " bytes, Start : " + fastestPath.get(0) + " , Destinaion : " + fastestPath.get(fastestPath.size() - 1));
//        Toast.makeText(this, "Dijkstra processing complete", Toast.LENGTH_SHORT).show();
    }

    private void runAStarPathfindingForThread(int timeInMinutes, LatLng startLatLng, AstarAlgorithm.HeuristicType heuristicType) {
        Runtime runtime = Runtime.getRuntime();
        CountDownLatch latch = new CountDownLatch(1);

        System.gc();
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {}

        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        long startTime = System.currentTimeMillis();

        Log.e("AstarDebug", "runOnUiThread akan dipanggil");

        runOnUiThread(() -> {
            try {
                Log.e("AstarDebug", "Masuk ke runOnUiThread");

                if (startLatLng == null) {
                    Toast.makeText(this, "Titik awal belum dipilih", Toast.LENGTH_SHORT).show();
                    Log.e("AstarError", "StartLatLng null");
                    return;
                }

                if (polyline == null || polyline.isEmpty()) {
                    Toast.makeText(this, "Polyline tidak tersedia, reset ulang aplikasi", Toast.LENGTH_SHORT).show();
                    Log.e("AstarError", "Polyline null atau kosong");
                    return;
                }

                for (Marker marker : dynamicMarkers) {
                    marker.remove();
                }
                dynamicMarkers.clear();

                if (currentPolygon != null) {
                    currentPolygon.remove();
                    currentPolygon = null;
                }

                List<LatLng> destinations = RouteCalculator.calculateDestinations(this, startLatLng, mMap, timeInMinutes);
                List<LatLng> updatedDestinations = GeofenceTime.updateDestinations(this, startLatLng, destinations, timeInMinutes, polyline);

                if (destinations == null || updatedDestinations == null) {
                    Toast.makeText(this, "Destinasi gagal dihitung. Coba ulangi", Toast.LENGTH_LONG).show();
                    Log.e("AstarError", "Destinations atau updatedDestinations null");
                    return;
                }

                if (updatedDestinations.size() != destinations.size()) {
                    Toast.makeText(this, "Data tidak valid", Toast.LENGTH_LONG).show();
                    Log.e("AstarError", "Ukuran updatedDestinations tidak sama dengan destinations");
                    return;
                }

                sortPointsToFormPolygon(updatedDestinations);
                drawGeofencePolygon(updatedDestinations);
                mapDriver.setGeofence(updatedDestinations);

                RoutePoint start = findRoutePoint(startLatLng);
                Map<String, AstarAlgorithm.Node> nodeMap = graph.toNodeMap();
                AstarAlgorithm.Node startNode = nodeMap.get(start.getId());

                List<LatLng> drivers = mapDriver.getDrivers(this, false);
                List<LatLng> fastestPath = new ArrayList<>();
                double shortestTime = Double.MAX_VALUE;
                LatLng fastestDriver = null;

                double shortestDistance = Double.MAX_VALUE;
                LatLng nearestDriver = null;

                for (LatLng driverLoc : drivers) {
                    // Hitung jarak Euclidean (langsung)
                    double distance = distanceInMeters(startLatLng, driverLoc);
                    if (distance < shortestDistance) {
                        shortestDistance = distance;
                        nearestDriver = driverLoc;
                    }

                    // A* Pathfinding
                    RoutePoint rpDriver = nearestRoutePoint(driverLoc);
                    AstarAlgorithm.Node goalNode = nodeMap.get(rpDriver.getId());
                    if (goalNode == null) continue;

                    List<LatLng> path = AstarAlgorithm.findPath(nodeMap, startNode, goalNode, heuristicType);
                    double pathLength = pathLengthInMinutes(path);

                    if (!path.isEmpty() && pathLength < shortestTime) {
                        shortestTime = pathLength;
                        fastestPath = path;
                        fastestDriver = driverLoc;
                    }
                }

                if (!fastestPath.isEmpty()) {
                    double distanceToFastest = distanceInMeters(startLatLng, fastestDriver);
                    Log.e("Proses-Astar-FastestDriver",
                            "Start: " + fastestPath.get(0) +
                                    " | End: " + fastestPath.get(fastestPath.size() - 1) +
                                    " | Fastest Driver: " + fastestDriver +
                                    " | Waktu (menit): " + shortestTime +
                                    " | Jarak (meter): " + distanceToFastest);
                }

                if (nearestDriver != null) {
                    Log.e("Proses-Astar-NearestDriver",
                            "Nearest Driver (jarak langsung): " + nearestDriver +
                                    " | Jarak: " + shortestDistance + " meter");
                }

            } catch (Exception e) {
                Log.e("AstarException", "Terjadi error di runOnUiThread:", e);
            } finally {
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                Log.e("AstarTimeout", "runOnUiThread tidak selesai dalam 10 detik");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e("AstarInterrupted", "Thread terinterupsi saat menunggu latch", e);
        }

        System.gc();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}

        long endTime = System.currentTimeMillis();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

        long memoryUsed = memoryAfter - memoryBefore;
        long timeElapsed = endTime - startTime;

        Log.e("Proses-Astar-" + heuristicType.name(), "Time: " + timeElapsed + " ms, Memory: " + memoryUsed + " bytes");
    }

    private void runDijkstraPathfindingForThread(int timeInMinutes, LatLng startLatLng) {
        Runtime runtime = Runtime.getRuntime();
        CountDownLatch latch = new CountDownLatch(1);

        System.gc();
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {}

        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        long startTime = System.currentTimeMillis();

        Log.e("DijkstraDebug", "runOnUiThread akan dipanggil");

        runOnUiThread(() -> {
            try {
                Log.e("DijkstraDebug", "Masuk ke runOnUiThread");

                List<LatLng> destinations = RouteCalculator.calculateDestinations(this, startLatLng, mMap, timeInMinutes);
                List<LatLng> updatedDestinations = GeofenceTime.updateDestinations(this, startLatLng, destinations, timeInMinutes, polyline);

                sortPointsToFormPolygon(updatedDestinations);
                mapDriver.setGeofence(updatedDestinations);
                RoutePoint start = findRoutePoint(startLatLng);
                Map<String, DjikstraAlgorithm.Node> nodeMap = graph.toDijkstraNodeMap();
                DjikstraAlgorithm.Node startNode = nodeMap.get(start.getId());

                List<LatLng> drivers = mapDriver.getDrivers(this, false);
                List<LatLng> fastestPath = new ArrayList<>();
                double shortestTime = Double.MAX_VALUE;
                LatLng fastestDriver = null;

                double shortestDistance = Double.MAX_VALUE;
                LatLng nearestDriver = null;

                for (LatLng driverLoc : drivers) {
                    // Hitung jarak Euclidean
                    double distance = distanceInMeters(startLatLng, driverLoc);
                    if (distance < shortestDistance) {
                        shortestDistance = distance;
                        nearestDriver = driverLoc;
                    }

                    // Hitung waktu dengan Dijkstra
                    RoutePoint rpDriver = nearestRoutePoint(driverLoc);
                    DjikstraAlgorithm.Node goalNode = nodeMap.get(rpDriver.getId());
                    if (goalNode == null) continue;

                    List<LatLng> path = DjikstraAlgorithm.findPath(nodeMap, startNode, goalNode);
                    double pathLength = pathLengthInMinutes(path);

                    if (!path.isEmpty() && pathLength < shortestTime) {
                        shortestTime = pathLength;
                        fastestPath = path;
                        fastestDriver = driverLoc;
                    }
                }

                if (!fastestPath.isEmpty()) {
                    double distanceToFastest = distanceInMeters(startLatLng, fastestDriver);
                    mMap.clear();
//                    mMap.addMarker(new MarkerOptions().position(fastestPath.get(0)).title("Start" + startLatLng));
//                    mMap.addMarker(new MarkerOptions().position(fastestPath.get(fastestPath.size() - 1)).title("Fastest Driver" + fastestPath.get(fastestPath.size() - 1)));
//                    mMap.addMarker(new MarkerOptions().position(fastestDriver));
//                    mMap.addPolyline(new com.google.android.gms.maps.model.PolylineOptions().addAll(fastestPath));
                    Log.e("Proses-Dijkstra-FastestDriver",
                            "Start: " + fastestPath.get(0) +
                                    " | End: " + fastestPath.get(fastestPath.size() - 1) +
                                    " | Fastest Driver: " + fastestDriver +
                                    " | Waktu (menit): " + shortestTime +
                                    " | Jarak (meter): " + distanceToFastest);
                }

                if (nearestDriver != null) {
                    Log.e("Proses-Dijkstra-NearestDriver",
                            "Nearest Driver (jarak langsung): " + nearestDriver +
                                    " | Jarak: " + shortestDistance + " meter");
                }

            } catch (Exception e) {
                Log.e("DijkstraException", "Terjadi error di runOnUiThread:", e);
            } finally {
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                Log.e("DijkstraTimeout", "runOnUiThread tidak selesai dalam 10 detik");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e("DijkstraInterrupted", "Thread terinterupsi saat menunggu latch", e);
        }

        System.gc();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}

        long endTime = System.currentTimeMillis();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

        long memoryUsed = memoryAfter - memoryBefore;
        long timeElapsed = endTime - startTime;

        Log.e("Proses-Dijkstra", "Waktu: " + timeElapsed + " ms, Memori: " + memoryUsed + " bytes");
    }

    public void runMultipleDijkstraPathfinding(int timeInMinutes, LatLng startLatLng) {
        Thread[] threads = new Thread[30];  // Array untuk menyimpan thread

        for (int i = 0; i < 30; i++) {
            threads[i] = new Thread(() -> {
                runDijkstraPathfindingForThread(timeInMinutes, startLatLng);
                Log.d("Running", "runMultipleDijkstraPathfinding Running");
            });
            threads[i].start();  // Memulai thread
        }

        for (int i = 0; i < 30; i++) {
            try {
                threads[i].join();  // Menunggu setiap thread selesai
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Menangani interrupt
                System.out.println("Thread interrupted: " + e.getMessage());
            }
        }

        System.out.println("All threads have completed execution.");
    }

    public void runMultipleAstarPathfinding(int timeInMinutes, LatLng startLatLng, AstarAlgorithm.HeuristicType heuristicType) {
        Thread[] threads = new Thread[30];  // Array untuk menyimpan thread

        for (int i = 0; i < 30; i++) {
            threads[i] = new Thread(() -> {
                runAStarPathfindingForThread(timeInMinutes, startLatLng, heuristicType);
            });
            threads[i].start();  // Memulai thread
        }

        for (int i = 0; i < 30; i++) {
            try {
                threads[i].join();  // Menunggu setiap thread selesai
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Menangani interrupt
                System.out.println("Thread interrupted: " + e.getMessage());
            }
        }

        System.out.println("All threads have completed execution.");
    }

    private double pathLengthInMinutes(List<LatLng> path) {
        double total = 0;
        for (int i = 1; i < path.size(); i++) {
            total += euclideanDistance(path.get(i - 1), path.get(i));
        }
        return total;
    }
    private double distanceInMeters(LatLng start, LatLng end) {
        float[] results = new float[1];
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results);
        return results[0]; // dalam meter
    }

    private RoutePoint nearestRoutePoint(LatLng latLng) {
        List<RoutePoint> points = RouteLoader.getAllRoutePoints();
        if (points == null || points.isEmpty()) {
            return null;
        }

        RoutePoint nearestPoint = null;
        double minDistance = Double.MAX_VALUE;

        // Menggunakan priority queue untuk menyimpan pasangan jarak dan point berdasarkan jarak terkecil
        PriorityQueue<RoutePoint> queue = new PriorityQueue<>(points.size(), new Comparator<RoutePoint>() {
            @Override
            public int compare(RoutePoint p1, RoutePoint p2) {
                double dist1 = euclideanDistance(latLng, new LatLng(p1.getLat(), p1.getLng()));
                double dist2 = euclideanDistance(latLng, new LatLng(p2.getLat(), p2.getLng()));
                return Double.compare(dist1, dist2);
            }
        });

        // Menambahkan semua RoutePoints ke dalam priority queue
        for (RoutePoint point : points) {
            queue.add(point);
        }

        // Mengambil RoutePoint dengan jarak terkecil
        nearestPoint = queue.poll();

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
        mapDriver = new MapDriver(mMap);  // Initialize with the ready map
        mapDriver.displayAllDriversOnMap(this);

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

        List<RouteLoader.Interchange> interchanges = RouteLoader.loadInterchanges(this, routes);
        graph = new Graph(pointsCounter);

        for (Route route : routes) {
            List<RoutePoint> points = route.getRoutePoints();
            for (int i = 0; i < points.size() - 1; i++) {
                for (RouteLoader.Interchange interchange : interchanges) {
                    graph.addEdge(points.get(i), points.get(i + 1));
                    graph.addEdge(points.get(i + 1), points.get(i));

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
