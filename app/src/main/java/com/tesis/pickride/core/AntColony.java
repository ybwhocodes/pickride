package com.tesis.pickride.core;

import com.google.android.gms.maps.model.LatLng;

import java.util.*;

public class AntColony {
    public static class Node {
        public String id;
        public LatLng position;
        public List<Edge> neighbors = new ArrayList<>();

        public Node(String id, LatLng position) {
            this.id = id;
            this.position = position;
        }
    }

    public static class Edge {
        public Node to;
        public double cost;
        public double pheromone;

        public Edge(Node to, double cost) {
            this.to = to;
            this.cost = cost;
            this.pheromone = 1.0;
        }
    }

    private static class Ant {
        public List<Node> path = new ArrayList<>();
        public Set<String> visited = new HashSet<>();
        public double totalCost = 0;

        public void visit(Node node) {
            path.add(node);
            visited.add(node.id);
        }
    }

    public static List<LatLng> findPath(Map<String, Node> graph, Node start, Node goal,
                                        int numAnts, int maxIterations, double alpha, double beta,
                                        double evaporationRate, double q) {
        Random random = new Random();
        List<Node> bestPath = new ArrayList<>();
        double bestCost = Double.MAX_VALUE;

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            List<Ant> ants = new ArrayList<>();

            for (int k = 0; k < numAnts; k++) {
                Ant ant = new Ant();
                ant.visit(start);
                Node current = start;

                while (!current.id.equals(goal.id)) {
                    List<Edge> candidates = current.neighbors.stream()
                            .filter(e -> !ant.visited.contains(e.to.id))
                            .toList();

                    if (candidates.isEmpty()) break;

                    double total = 0.0;
                    for (Edge edge : candidates) {
                        total += Math.pow(edge.pheromone, alpha) * Math.pow(1.0 / edge.cost, beta);
                    }

                    double r = random.nextDouble();
                    double cumulative = 0.0;
                    Node next = null;

                    for (Edge edge : candidates) {
                        cumulative += Math.pow(edge.pheromone, alpha) * Math.pow(1.0 / edge.cost, beta) / total;
                        if (r <= cumulative) {
                            next = edge.to;
                            ant.totalCost += edge.cost;
                            break;
                        }
                    }

                    if (next == null) break;

                    ant.visit(next);
                    current = next;
                }

                if (current.id.equals(goal.id) && ant.totalCost < bestCost) {
                    bestCost = ant.totalCost;
                    bestPath = new ArrayList<>(ant.path);
                }

                ants.add(ant);
            }

            for (Node node : graph.values()) {
                for (Edge edge : node.neighbors) {
                    edge.pheromone *= (1 - evaporationRate);
                }
            }

            for (Ant ant : ants) {
                if (ant.path.size() < 2) continue;
                for (int i = 0; i < ant.path.size() - 1; i++) {
                    Node from = ant.path.get(i);
                    Node to = ant.path.get(i + 1);
                    for (Edge edge : from.neighbors) {
                        if (edge.to.equals(to)) {
                            edge.pheromone += q / ant.totalCost;
                            break;
                        }
                    }
                }
            }
        }

        List<LatLng> pathLatLng = new ArrayList<>();
        for (Node node : bestPath) {
            pathLatLng.add(node.position);
        }
        return pathLatLng;
    }
}