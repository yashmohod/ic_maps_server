package com.ops.ICmaps.Edge;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.ops.ICmaps.NavMode.NavMode;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "edges")
public class Edge {

    @Id
    private String key;
    private String fromNode;
    private String toNode;

    private Boolean interBuildingEdge;
    private String buildingId;

    // any metadata: distance in meters, cost, speed limit, etc.
    private double distanceMeters;
    private boolean biDirectional;

    @ManyToMany
    @JoinTable(name = "edge_navmode", joinColumns = @JoinColumn(name = "edge_id"), inverseJoinColumns = @JoinColumn(name = "navmode_id"))
    private Set<NavMode> navModes = new HashSet<>();

    public boolean AddNavMode(NavMode curNavMode) {
        return navModes.add(curNavMode);
    }

    public boolean RemoveNavMode(NavMode curNavMode) {
        return navModes.remove(curNavMode);
    }

    public Set<NavMode> getNavModes() {
        return navModes;
    }

    protected Edge() {

    }

    public Edge(double distanceMeters, String toNode, String fromNode, String key, Boolean biDirectional,Boolean interBuildingEdge,String buildingId ) {
        this.distanceMeters = distanceMeters;
        this.toNode = toNode;
        this.fromNode = fromNode;
        this.key = key;
        this.biDirectional = biDirectional;
        this.interBuildingEdge = interBuildingEdge;
        this.buildingId = buildingId;
    }


    public Boolean isBiDirectional(){
        return this.biDirectional;
    }
    public Boolean isInterBuildingEdge(){
        return this.interBuildingEdge;
    }
    public void setBiDirectional(boolean val ){
        this.biDirectional = val;
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
