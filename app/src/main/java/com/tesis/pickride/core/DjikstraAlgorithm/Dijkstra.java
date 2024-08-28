package com.tesis.pickride.core.DjikstraAlgorithm;


import com.tesis.pickride.model.Edge;
import com.tesis.pickride.model.Node;
import com.tesis.pickride.utils.Graph;

import java.util.*;

public class Dijkstra {

    public static Map<Node, Double> dijkstra(Graph graph, Node start) {
        Map<Node, Double> distances = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparing(distances::get));
        distances.put(start, 0.0);

        pq.add(start);

        while (!pq.isEmpty()) {
            Node current = pq.poll();

            for (Edge edge : graph.getAdjList().get(current)) {
                Node neighbor = edge.destination;
                double newDist = distances.get(current) + edge.weight;

                if (newDist < distances.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    distances.put(neighbor, newDist);
                    pq.add(neighbor);
                }
            }
        }

        return distances;
    }
}
