package com.ops.ICmaps.Navigation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.ops.ICmaps.Edge.Edge;
import com.ops.ICmaps.Edge.EdgeRepository;
import com.ops.ICmaps.NavMode.NavMode;
import com.ops.ICmaps.Node.Node;
import com.ops.ICmaps.Node.NodeRepository;

@Service
public class GraphService {

    private final EdgeRepository edges;
    private final NodeRepository nodes;

    // Volatile so reads require no locking
    private volatile Map<String, List<Adj>> adj = Map.of();

    private volatile Map<String, double[]> nodeCoords = Map.of(); // id -> [lat,lng]

    public GraphService(EdgeRepository edges, NodeRepository nodes) {
        this.edges = edges;
        this.nodes = nodes;
    }

    public static record Adj(String to, double distance, Set<NavMode> navModes) {
    }

    public Map<String, List<Adj>> getGraph() {
        return adj;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadGraph() {
        rebuild();
        rebuildNodes(nodes.findAll());
    }

    private void rebuildNodes(List<Node> allNodes) {
        Map<String, double[]> m = new HashMap<>();
        for (var n : allNodes)
            m.put(n.getId(), new double[] { n.getLat(), n.getLng() });
        nodeCoords = Map.copyOf(m);
    }

    private String nearestNodeId(double lat, double lng) {
        String best = null;
        double bestD2 = Double.POSITIVE_INFINITY;
        for (var e : nodeCoords.entrySet()) {
            var c = e.getValue();
            double d2 = haversineSquared(lat, lng, c[0], c[1]); // or simple euclidean on small boxes
            if (d2 < bestD2) {
                bestD2 = d2;
                best = e.getKey();
            }
        }
        return best;
    }

    private static double haversineSquared(double lat1, double lon1, double lat2,
            double lon2) {
        // You can return actual meters too; squared used only if you compare.
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return (R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }

    private void rebuild() {
        Map<String, List<Adj>> map = new HashMap<>();

        for (Edge e : edges.findAll()) {
            map.computeIfAbsent(e.getFromNode(), k -> new ArrayList<>())
                    .add(new Adj(e.getToNode(), e.getDistance(), e.getNavModes()));
            map.computeIfAbsent(e.getToNode(), k -> new ArrayList<>())
                    .add(new Adj(e.getFromNode(), e.getDistance(), e.getNavModes()));
        }

        // freeze lists + map for thread safety, zero lock reads
        map.replaceAll((k, v) -> List.copyOf(v));
        adj = Map.copyOf(map);
    }

    // For A* / Dijkstra expansions
    public List<Adj> neighbors(String fromId) {
        return adj.getOrDefault(fromId, List.of());
    }

    public String[] navigate(double lat, double lng, String DestinationId, Long navModeId) {

        String startId = nearestNodeId(lat, lng);

        Map<String, String> path = Astar(startId, DestinationId, navModeId);
        String cur = DestinationId;
        String pathEdges[] = new String[path.size() - 1];
        for (int i = 0; i < pathEdges.length - 1; i += 1) {
            String nxt = path.get(cur);
            if (!edges.findByFromNodeAndToNode(cur, nxt).isEmpty()) {

            } else if (!edges.findByFromNodeAndToNode(nxt, cur).isEmpty()) {

            } else {
                return null;
            }
            cur = nxt;
        }

        return pathEdges;
    }

    private Map<String, String> Astar(String start, String end, Long navModeId) {
        if (start.equals(end)) {
            return null;
        }

        // f = g + h
        record State(String id, double f) {
        }
        PriorityQueue<State> open = new PriorityQueue<>(Comparator.comparingDouble(State::f));
        Map<String, Double> g = new HashMap<>();
        Map<String, String> parent = new HashMap<>();
        Set<String> closed = new HashSet<>();

        g.put(start, 0.0);
        open.add(new State(start, heuristic(start, end)));

        while (!open.isEmpty()) {
            String cur = open.poll().id();
            if (!closed.add(cur))
                continue; // skip if already expanded
            if (cur.equals(end))
                return parent;

            for (Adj e : neighbors(cur)) {
                String nxt = e.to(); // Adj(to, distance)
                if (closed.contains(nxt))
                    continue;

                double tentative = g.get(cur) + e.distance();
                if (tentative < g.getOrDefault(nxt, Double.POSITIVE_INFINITY)) {
                    g.put(nxt, tentative);
                    parent.put(nxt, cur);
                    double f = tentative + heuristic(nxt, end);
                    open.add(new State(nxt, f));
                }
            }
        }
        return null;
    }

    private double heuristic(String cur, String end) {
        double start_ll[] = nodeCoords.get(cur);
        double end_ll[] = nodeCoords.get(end);
        double result = Math.sqrt(Math.pow(start_ll[0] - end_ll[0], 2) +
                Math.pow(start_ll[1] - end_ll[1], 2));
        return result;
    }

    // distane in meters
    public Double calDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        Double R = 6371.0; // Radius of the earth in km
        Double dLat = Math.toRadians(lat2 - lat1); // deg2rad below
        Double dLon = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        Double d = R * c; // Distance in km
        return d * 1000; // Distance in km
    }
}
