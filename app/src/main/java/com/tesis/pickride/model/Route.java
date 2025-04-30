package com.tesis.pickride.model;
import java.util.List;

public class Route {
    private String routeName;
    private List<RoutePoint> routePoints;

    public Route(String routeName, List<RoutePoint> routePoints) {
        this.routeName = routeName;
        this.routePoints = routePoints;
    }

    public String getRouteName() {
        return routeName;
    }

    public List<RoutePoint> getRoutePoints() {
        return routePoints;
    }

}