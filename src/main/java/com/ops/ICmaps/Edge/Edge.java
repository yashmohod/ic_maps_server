package com.ops.ICmaps.Edge;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "edges")
public class Edge {

    @Id
    private String key;
    private String fromNode;
    private String toNode;

    // any metadata: distance in meters, cost, speed limit, etc.
    private double distanceMeters;

    protected Edge() {

    }

    public Edge(double distanceMeters, String toNode, String fromNode, String key) {
        this.distanceMeters = distanceMeters;
        this.toNode = toNode;
        this.fromNode = fromNode;
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String getFromNode() {
        return this.fromNode;
    }

    public void setFromNode(String fromNode) {
        this.fromNode = fromNode;
    }

    public String getToNode() {
        return this.toNode;
    }

    public void setToNode(String toNode) {
        this.toNode = toNode;
    }

    public double getDistance() {
        return distanceMeters;
    }

    public void setDistance(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Edge edge = (Edge) o;
        return Double.compare(distanceMeters, edge.distanceMeters) == 0 && Objects.equals(key, edge.key)
                && Objects.equals(fromNode, edge.fromNode) && Objects.equals(toNode, edge.toNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, fromNode, toNode, distanceMeters);
    }
}
