package com.tesis.pickride.core;

import com.tesis.pickride.model.Edge;
import com.tesis.pickride.model.RoutePoint;
import com.tesis.pickride.utils.RouteCalculator;

import java.util.*;

public class Graph {
  public int vertices;
  public Map<RoutePoint, LinkedList<Edge>> adjacencyList; // Change to Map for RoutePoint

  public Graph(int vertices) {
    this.vertices = vertices;
    adjacencyList = new HashMap<>();
  }

  public void addEdge(RoutePoint src, RoutePoint dest) {
    adjacencyList.putIfAbsent(src, new LinkedList<>());

    double distance = RouteCalculator.calculateDistanceInMeters(src, dest); // meter
    double speed = RouteCalculator.getSpeedInKmPerHour(dest.getSpeed()); // kmh
    double time = (distance / (speed * (1000.0 / 3600.0))) / 60.0; // menit

    adjacencyList.get(src).add(new Edge(dest, time));
  }

  public Map<RoutePoint, Double> dijkstra(RoutePoint startVertex) {
    // Change the distances map to use Double
    Map<RoutePoint, Double> distances = new HashMap<>();
    Map<RoutePoint, Boolean> visited = new HashMap<>();
    // Use Comparator.comparingDouble() for double weights
    PriorityQueue<Edge> pq = new PriorityQueue<>(Comparator.comparingDouble(edge -> edge.weight));

    // Initialize distances and visited maps
    for (RoutePoint vertex : adjacencyList.keySet()) {
      distances.put(vertex, Double.MAX_VALUE); // Set initial distance to infinity
      visited.put(vertex, false);
    }

    distances.put(startVertex, 0.0); // Starting vertex has a distance of 0
    pq.add(new Edge(startVertex, 0.0)); // Add the start vertex to the priority queue

    while (!pq.isEmpty()) {
      Edge currentEdge = pq.poll();
      RoutePoint u = currentEdge.vertex;

      if (visited.get(u)) continue; // Skip already visited vertices
      visited.put(u, true);

      // Iterate through all adjacent edges
      for (Edge edge : adjacencyList.get(u)) {
        RoutePoint v = edge.vertex;
        double weight = edge.weight;

        // Relaxation step: if a shorter path is found
        if (!visited.get(v) && distances.get(u) + weight < distances.get(v)) {
          distances.put(v, distances.get(u) + weight);
          pq.add(new Edge(v, distances.get(v))); // Add updated distance to the priority queue
        }
      }
    }
    return distances; // Return the shortest distances from startVertex to all other vertices
  }
}