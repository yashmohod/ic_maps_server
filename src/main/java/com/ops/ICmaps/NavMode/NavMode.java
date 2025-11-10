package com.ops.ICmaps.NavMode;

import com.ops.ICmaps.Edge.Edge;
import com.ops.ICmaps.Node.Node;
import jakarta.persistence.*;

import java.util.*;

@Entity
@Table(name = "navmode")
public class NavMode {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;
        private String name;

        protected NavMode() {

        }

        public NavMode(Long id, String name) {
                this.id = id;
                this.name = name;
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

        @ManyToMany
        @JoinTable(name = "node_navmode", joinColumns = @JoinColumn(name = "navmode_id"), inverseJoinColumns = @JoinColumn(name = "node_id"))
        Set<Node> nodes;

        @ManyToMany
        @JoinTable(name = "edge_navmode", joinColumns = @JoinColumn(name = "navmode_id"), inverseJoinColumns = @JoinColumn(name = "edge_id"))
        Set<Edge> edges;

}
