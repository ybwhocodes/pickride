package com.tesis.pickride.core;


import com.google.android.gms.maps.model.LatLng;

import java.util.*;


public class DjikstraAlgorithm {

    public static class Node {
        public String id;
        public LatLng position;
        public List<Edge> neighbors = new ArrayList<>();
        public double distance = Double.MAX_VALUE;
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

    public static List<LatLng> findPath(Map<String, Node> graph, Node start, Node goal) {
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingDouble(n -> n.distance));
        Set<String> visited = new HashSet<>();

        for (Node node : graph.values()) {
            node.distance = Double.MAX_VALUE;
            node.parent = null;
        }

        start.distance = 0;
        queue.add(start);

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            if (current.id.equals(goal.id)) {
                return reconstructPath(current);
            }

            visited.add(current.id);

            for (Edge edge : current.neighbors) {
                Node neighbor = edge.to;
                if (visited.contains(neighbor.id)) continue;

                double newDist = current.distance + edge.cost;
                if (newDist < neighbor.distance) {
                    neighbor.distance = newDist;
                    neighbor.parent = current;
                    queue.add(neighbor);
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
}
