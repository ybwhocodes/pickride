package com.tesis.pickride.utils;


import android.content.Context;
import com.tesis.pickride.model.Route;
import com.tesis.pickride.model.RoutePoint;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RouteLoader {
    private static List<Route> routes = new ArrayList<>();

    public static List<Route> loadRoutes(Context context) {
        if (routes.isEmpty()) {
            String jsonStr = loadJSONFromAsset(context, "map-route.json");

            if (jsonStr != null) {
                try {
                    JSONArray jsonArray = new JSONArray(jsonStr);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject routeObj = jsonArray.getJSONObject(i);
                        String routeName = routeObj.getString("Route");
                        JSONArray pointsArray = routeObj.getJSONArray("Routes");
                        List<RoutePoint> points = new ArrayList<>();
                        for (int j = 0; j < pointsArray.length(); j++) {
                            JSONObject pointObj = pointsArray.getJSONObject(j);
                            RoutePoint point = new RoutePoint(
                                    pointObj.getString("id"),
                                    pointObj.getDouble("lat"),
                                    pointObj.getDouble("lng"),
                                    pointObj.getString("speed")
                            );
                            points.add(point);
                        }
                        routes.add(new Route(routeName, points));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return routes;
    }

    public static List<RoutePoint> getAllRoutePoints() {
        List<RoutePoint> allPoints = new ArrayList<>();
        for (Route route : routes) {
            allPoints.addAll(route.getRoutePoints());
        }
        return allPoints;
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
        }
        return json;
    }
}
