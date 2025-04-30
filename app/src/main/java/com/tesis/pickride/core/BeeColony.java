package com.tesis.pickride.core;

import com.google.android.gms.maps.model.LatLng;

import java.util.*;

public class BeeColony {
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

        public Edge(Node to, double cost) {
            this.to = to;
            this.cost = cost;
        }
    }

    private static class Bee {
        public List<Node> path = new ArrayList<>();
        public Set<String> visited = new HashSet<>();
        public double totalCost = 0;

        public void visit(Node node) {
            path.add(node);
            visited.add(node.id);
        }
    }

    public static List<LatLng> findPath(Map<String, Node> graph, Node start, Node goal,
                                        int numBees, int maxIterations, int eliteSites,
                                        int selectedSites, int neighborhoodSize) {
        Random random = new Random();
        List<Node> bestPath = new ArrayList<>();
        double bestCost = Double.MAX_VALUE;

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            List<Bee> allBees = new ArrayList<>();
            List<Bee> candidates = new ArrayList<>();

            for (int i = 0; i < numBees; i++) {
                Bee bee = explore(graph, start, goal, neighborhoodSize, random);
                if (bee != null && bee.totalCost < bestCost) {
                    bestCost = bee.totalCost;
                    bestPath = new ArrayList<>(bee.path);
                }
                allBees.add(bee);
            }

            allBees.sort(Comparator.comparingDouble(b -> b.totalCost));
            candidates.addAll(allBees.subList(0, Math.min(selectedSites, allBees.size())));

            for (int i = 0; i < eliteSites && i < candidates.size(); i++) {
                Bee elite = candidates.get(i);
                for (int j = 0; j < 3; j++) {
                    Bee bee = localSearch(elite, goal, neighborhoodSize, random);
                    if (bee != null && bee.totalCost < bestCost) {
                        bestCost = bee.totalCost;
                        bestPath = new ArrayList<>(bee.path);
                    }
                }
            }
        }

        List<LatLng> result = new ArrayList<>();
        for (Node node : bestPath) {
            result.add(node.position);
        }
        return result;
    }

    private static Bee explore(Map<String, Node> graph, Node start, Node goal, int depth, Random random) {
        Bee bee = new Bee();
        bee.visit(start);
        Node current = start;

        for (int i = 0; i < depth && !current.id.equals(goal.id); i++) {
            List<Edge> options = current.neighbors.stream()
                    .filter(e -> !bee.visited.contains(e.to.id))
                    .toList();
            if (options.isEmpty()) break;
            Edge next = options.get(random.nextInt(options.size()));
            bee.visit(next.to);
            bee.totalCost += next.cost;
            current = next.to;
        }

        return current.id.equals(goal.id) ? bee : null;
    }

    private static Bee localSearch(Bee original, Node goal, int depth, Random random) {
        Node start = original.path.get(0);
        return explore(buildSubgraph(original), start, goal, depth, random);
    }

    private static Map<String, Node> buildSubgraph(Bee bee) {
        Map<String, Node> subgraph = new HashMap<>();
        for (Node n : bee.path) {
            Node copy = new Node(n.id, n.position);
            subgraph.put(n.id, copy);
        }
        for (Node n : bee.path) {
            Node from = subgraph.get(n.id);
            for (Edge e : n.neighbors) {
                if (subgraph.containsKey(e.to.id)) {
                    from.neighbors.add(new Edge(subgraph.get(e.to.id), e.cost));
                }
            }
        }
        return subgraph;
    }
}
