package com.tesis.pickride.core;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.tesis.pickride.model.Edge;
import com.tesis.pickride.model.RoutePoint;
import com.tesis.pickride.utils.RouteCalculator;
import com.tesis.pickride.utils.RouteLoader;

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

//    Log.d("Graph-Edge", "Edge added: " + src.getId() + " -> " + dest.getId()
//            + " | Distance: " + distance + " m"
//            + " | Speed: " + speed + " km/h"
//            + " | Time: " + time + " min");

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
  public List<RoutePoint> getNeighbors(RoutePoint point) {
    List<RoutePoint> neighbors = new ArrayList<>();
    LinkedList<Edge> edges = adjacencyList.get(point);
    if (edges != null) {
      for (Edge edge : edges) {
        neighbors.add(edge.vertex);
      }
    }
    return neighbors;
  }
  // Tambahkan di akhir file Graph.java
  public Map<String, AstarAlgorithm.Node> toNodeMap() {
    Map<String, AstarAlgorithm.Node> nodeMap = new HashMap<>();

    // Buat node kosong dari RoutePoint
    for (RoutePoint rp : getAllRoutePoints()) {
      LatLng pos = new LatLng(rp.getLat(), rp.getLng());
      AstarAlgorithm.Node node = new AstarAlgorithm.Node(rp.getId(), pos);
      nodeMap.put(rp.getId(), node);
    }

    // Tambahkan edge ke node
    for (RoutePoint from : getAllRoutePoints()) {
      AstarAlgorithm.Node fromNode = nodeMap.get(from.getId());
      for (RoutePoint to : getNeighbors(from)) {
        AstarAlgorithm.Node toNode = nodeMap.get(to.getId());
        double cost = euclideanDistance(from, to);
        fromNode.neighbors.add(new AstarAlgorithm.Edge(toNode, cost));
      }
    }

    return nodeMap;
  }

  private List<RoutePoint> getAllRoutePoints() {
    return RouteLoader.getAllRoutePoints(); // Asumsikan ini sudah ada
  }

  private double euclideanDistance(RoutePoint a, RoutePoint b) {
    double dx = a.getLat() - b.getLat();
    double dy = a.getLng() - b.getLng();
    return Math.sqrt(dx * dx + dy * dy);
  }
  public Map<String, DjikstraAlgorithm.Node> toDijkstraNodeMap() {
    Map<String, DjikstraAlgorithm.Node> nodeMap = new HashMap<>();

    for (RoutePoint rp : RouteLoader.getAllRoutePoints()) {
      LatLng pos = new LatLng(rp.getLat(), rp.getLng());
      DjikstraAlgorithm.Node node = new DjikstraAlgorithm.Node(rp.getId(), pos);
      nodeMap.put(rp.getId(), node);
    }

    for (Map.Entry<RoutePoint, LinkedList<Edge>> entry : adjacencyList.entrySet()) {
      RoutePoint from = entry.getKey();
      DjikstraAlgorithm.Node fromNode = nodeMap.get(from.getId());
      for (Edge edge : entry.getValue()) {
        RoutePoint to = edge.vertex;
        DjikstraAlgorithm.Node toNode = nodeMap.get(to.getId());
        if (toNode != null) {
          fromNode.neighbors.add(new DjikstraAlgorithm.Edge(toNode, edge.weight));
        }
      }
    }

    return nodeMap;
  }


}