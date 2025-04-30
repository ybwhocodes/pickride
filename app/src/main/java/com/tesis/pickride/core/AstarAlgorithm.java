package com.tesis.pickride.core;
import com.google.android.gms.maps.model.LatLng;

import java.util.*;

public class AstarAlgorithm {
    public enum HeuristicType {
        MANHATTAN,
        EUCLIDEAN,
        HAVERSINE
    }

    public static class Node {
        public String id;
        public LatLng position;
        public List<Edge> neighbors = new ArrayList<>();
        public double g;
        public double h;
        public double f;
        public Node parent;

        public Node(String id, LatLng position) {
            this.id = id;
            this.position = position;
        }
    }

    public static class Edge {
        public Node to;
        public double cost;

        public Edge(Node to, double cost) {
            this.to = to;
            this.cost = cost;
        }
    }

    public static List<LatLng> findPath(Map<String, Node> graph, Node start, Node goal, HeuristicType heuristicType) {
        PriorityQueue<Node> openList = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Set<String> closedSet = new HashSet<>();

        for (Node node : graph.values()) {
            node.g = Double.MAX_VALUE;
            node.h = 0;
            node.f = 0;
            node.parent = null;
        }

        start.g = 0;
        start.h = calculateHeuristic(start.position, goal.position, heuristicType);
        start.f = start.h;
        openList.add(start);

        while (!openList.isEmpty()) {
            Node current = openList.poll();

            if (current.id.equals(goal.id)) {
                return reconstructPath(current);
            }

            closedSet.add(current.id);

            for (Edge edge : current.neighbors) {
                Node neighbor = edge.to;

                if (closedSet.contains(neighbor.id)) continue;

                double tentativeG = current.g + edge.cost;

                if (tentativeG < neighbor.g) {
                    neighbor.parent = current;
                    neighbor.g = tentativeG;
                    neighbor.h = calculateHeuristic(neighbor.position, goal.position, heuristicType);
                    neighbor.f = neighbor.g + neighbor.h;
                    if (!openList.contains(neighbor)) {
                        openList.add(neighbor);
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    private static List<LatLng> reconstructPath(Node node) {
        List<LatLng> path = new ArrayList<>();
        while (node != null) {
            path.add(node.position);
            node = node.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static double calculateHeuristic(LatLng a, LatLng b, HeuristicType type) {
        double dx = Math.abs(a.latitude - b.latitude);
        double dy = Math.abs(a.longitude - b.longitude);
        switch (type) {
            case MANHATTAN:
                return dx + dy;
            case EUCLIDEAN:
                return Math.sqrt(dx * dx + dy * dy);
            case HAVERSINE:
                return haversine(a.latitude, a.longitude, b.latitude, b.longitude);
            default:
                return 0;
        }
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of Earth in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
