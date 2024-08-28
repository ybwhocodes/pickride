package com.tesis.pickride.model;

public class Node {
    public String id;
    double lat;
    double lng;
    String speed;

    public Node(String id, double lat, double lng, String speed) {
        this.id = id;
        this.lat = lat;
        this.lng = lng;
        this.speed = speed;
    }
}
