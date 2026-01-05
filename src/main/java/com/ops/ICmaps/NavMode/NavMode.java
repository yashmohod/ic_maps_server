package com.ops.ICmaps.NavMode;

import java.util.HashSet;
import java.util.Set;

import com.ops.ICmaps.Edge.Edge;
import com.ops.ICmaps.Node.Node;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "navmode")
public class NavMode {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;
        @Column(name="name", unique=true)
        private String name;
        private boolean fromThrough;

        protected NavMode() {

        }

        public NavMode(Long id, String name,boolean fromThrough) {
                this.id = id;
                this.name = name;
                this.fromThrough = fromThrough;
        }

        public boolean isFromThrough(){
                return this.fromThrough;
        }

        public void setIsFromThrough(boolean fromThrough){
                this.fromThrough = fromThrough;
        }

        public Long getId() {
                return this.id;
        }

        public String getName() {
                return this.name;
        }

        public void setName(String name) {
                this.name = name;
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

        public Boolean containsNode(Node node) {
                return this.nodes.contains(node);
        }

        public Set<Edge> getEdges() {
                return this.edges;
        }

        public void addEdge(Edge edge) {
                this.edges.add(edge);
        }

        public void removeEdge(Edge edge) {
                this.edges.remove(edge);
        }

        public Boolean containsEdge(Edge edge) {
                return this.edges.contains(edge);
        }

        @ManyToMany(mappedBy = "navModes")
        private Set<Node> nodes = new HashSet<>();

        @ManyToMany(mappedBy = "navModes")
        private Set<Edge> edges = new HashSet<>();

}
