package com.tesis.pickride.model;

public class RoutePoint {
    private String id;
    private double lat;
    private double lng;
    private String speed;

    public RoutePoint(String id, double lat, double lng, String speed) {
        this.id = id;
        this.lat = lat;
        this.lng = lng;
        this.speed = speed;
    }

    public String getId() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public String getSpeed() {
        return speed;
    }
}