package com.ops.ICmaps.Node;

import com.ops.ICmaps.Buildings.Building;
import com.ops.ICmaps.Edge.Edge;
import com.ops.ICmaps.NavMode.NavMode;


import jakarta.persistence.*;

import java.util.*;

@Entity
@Table(name = "nodes", indexes = {
        @Index(name = "idx_nodes_lat_lng", columnList = "lat,lng")
})
public class Node {

    @Id
    private String id;
    private Double lat;
    private Double lng;
    private boolean blueLight;
    // private boolean ada;

    @ManyToMany
    @JoinTable(name = "node_navmode", joinColumns = @JoinColumn(name = "node_id"), inverseJoinColumns = @JoinColumn(name = "navmode_id"))
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
    

    @ManyToMany(mappedBy = "nodes")
    Set<Building> buildings;

    protected Node() {
    }

    public Node(String id, Double lng, Double lat) {
        this.lng = lng;
        this.lat = lat;
        this.id = id; 
        this.blueLight = false;
    }



    public String getId() {
        return id;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public boolean isBlueLight() {
        return blueLight;
    }

    public void setBlueLight(boolean blueLight) {
        this.blueLight = blueLight;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Node node = (Node) o;
        return Objects.equals(id, node.id) && Objects.equals(lat, node.lat) && Objects.equals(lng, node.lng);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, lat, lng);
    }
}
