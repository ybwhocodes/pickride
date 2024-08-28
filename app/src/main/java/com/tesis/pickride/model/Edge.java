package com.tesis.pickride.model;

public class Edge {
    Node source;
    public Node destination;
    public double weight;

    public Edge(Node source, Node destination, double weight) {
        this.source = source;
        this.destination = destination;
        this.weight = weight;
    }
}
