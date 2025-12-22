package com.ops.ICmaps.Buildings;

import java.util.Set;

import com.ops.ICmaps.Node.Node;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "building")
public class Building {

    @Id
    private String id;
    private String name;
    private String polyGon;
    private Double lat;
    private Double lng;

    @ManyToMany
    @JoinTable(
            name = "node_building",
            joinColumns = @JoinColumn(name = "building_id"),
            inverseJoinColumns = @JoinColumn(name = "node_id"))
    Set<Node> nodes;

    protected Building() {
    }


    public Building(String id, String name,Double lat, Double lng, String polyGon) {
        this.name = name;
        this.id = id;
        this.lat =lat;
        this.lng =lng;
        this.polyGon=polyGon;
    }

    public String getId() {
        return this.id;
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

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPolyGon() {
        return this.polyGon;
    }

    public void setPolyGon(String polyGon) {
        this.polyGon = polyGon;
    }

    public Set<Node> getNodes() {
        return this.nodes;
    }

    public void addNode(Node node) {
        this.nodes.add(node);
    }

    public void removeNode(Node node) {
        this.nodes.remove(node);
    }

}
