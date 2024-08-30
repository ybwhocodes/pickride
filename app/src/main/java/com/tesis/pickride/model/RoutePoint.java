package com.tesis.pickride.model;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoutePoint that = (RoutePoint) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RoutePoint{" +
            "id='" + id + '\'' +
            ", lat=" + lat +
            ", lng=" + lng +
            ", speed='" + speed + '\'' +
            '}';
    }
}