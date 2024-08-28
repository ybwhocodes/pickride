package com.tesis.pickride.utils;

import com.tesis.pickride.model.Edge;
import com.tesis.pickride.model.Node;

import java.util.*;

public class Graph {
    private Map<String, Node> nodes = new HashMap<>();
    private Map<Node, List<Edge>> adjList = new HashMap<>();

    public void addNode(Node node) {
        nodes.put(node.id, node);
        adjList.putIfAbsent(node, new ArrayList<>());
    }

    public void addEdge(Node source, Node destination, double weight) {
        adjList.get(source).add(new Edge(source, destination, weight));
    }

    public Map<String, Node> getNodes() {
        return nodes;
    }

    public Map<Node, List<Edge>> getAdjList() {
        return adjList;
    }
}
