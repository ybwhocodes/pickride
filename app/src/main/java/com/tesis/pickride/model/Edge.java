package com.tesis.pickride.model;

import java.util.Comparator;

public class Edge implements Comparator<Edge> {
    public RoutePoint vertex; // Change from int to RoutePoint
    public double weight;

    public Edge() {
    }

    public Edge(RoutePoint vertex, double weight) {
        this.vertex = vertex;
        this.weight = weight;
    }

    @Override
    public int compare(Edge edge1, Edge edge2) {
        return Double.compare(edge1.weight, edge2.weight);
    }
}