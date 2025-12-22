package com.ops.ICmaps.Navigation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ops.ICmaps.Buildings.BuildingRepository;
import com.ops.ICmaps.Edge.Edge;
import com.ops.ICmaps.Edge.EdgeRepository;
import com.ops.ICmaps.NavMode.NavMode;
import com.ops.ICmaps.NavMode.NavModeRepository;
import com.ops.ICmaps.Node.Node;
import com.ops.ICmaps.Node.NodeRepository;

@Service
public class GraphService {

    private final EdgeRepository edges;
    private final NodeRepository nodes;
    private final NavModeRepository navr;
    private final BuildingRepository building;

    // Volatile so reads require no locking
    private volatile Map<String, List<Adj>> adj = Map.of();

    private volatile Map<String, double[]> nodeCoords = Map.of(); // id -> [lat,lng]

    public GraphService(BuildingRepository building, NavModeRepository navr, EdgeRepository edges,
            NodeRepository nodes) {
        this.navr = navr;
        this.building = building;
        this.edges = edges;
        this.nodes = nodes;
    }

    public static record Adj(String to, double distance, Set<Long> navModes,Boolean fromThrough) {

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
        // // You can return actual meters too; squared used only if you compare.
        // double R = 6371000.0;
        // double dLat = Math.toRadians(lat2 - lat1);
        // double dLon = Math.toRadians(lon2 - lon1);
        // double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        // + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        // * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        // return (R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
        return Math.sqrt(Math.pow(lat1 - lat2, 2) + Math.pow(lon2 - lon1, 2));
    }

    @Transactional(readOnly = true)
    private void rebuild() {
        Map<String, List<Adj>> map = new HashMap<>();

        for (Edge e : edges.findAllWithNavModes()) {
            Set<Long> navModesIds = e.getNavModes().stream()
                    .map(NavMode::getId)
                    .collect(Collectors.toUnmodifiableSet());
            if (e.isBiDirectional()) {
                map.computeIfAbsent(e.getFromNode(), k -> new ArrayList<>())
                        .add(new Adj(e.getToNode(), e.getDistance(), navModesIds,false));
                map.computeIfAbsent(e.getToNode(), k -> new ArrayList<>())
                        .add(new Adj(e.getFromNode(), e.getDistance(), navModesIds,false));
            } else {
                map.computeIfAbsent(e.getFromNode(), k -> new ArrayList<>())
                        .add(new Adj(e.getToNode(), e.getDistance(), navModesIds,false));
            }

        }

        for (Edge e : edges.findByInterBuildingEdge(true)) {

            map.computeIfAbsent(e.getFromNode(), k -> new ArrayList<>())
                    .add(new Adj(e.getToNode(), e.getDistance(), new HashSet<Long>(),true));
            map.computeIfAbsent(e.getToNode(), k -> new ArrayList<>())
                    .add(new Adj(e.getFromNode(), e.getDistance(), new HashSet<Long>(),true));


        }


        // freeze lists + map for thread safety, zero lock reads
        map.replaceAll((k, v) -> List.copyOf(v));
        adj = Map.copyOf(map);
    }

    // For A* / Dijkstra expansions
    public List<Adj> neighbors(String fromId) {
        return adj.getOrDefault(fromId, List.of());
    }
    public class ResponseObjectNavigateBlueLight {
    private Set<String> path;

    private String dest;

    public Set<String> getPath(){
        return this.path;
    }
    public String getDest(){
        return this.dest;
    }

    //accessors
    }

    public ResponseObjectNavigateBlueLight navigateBlueLight(double lat, double lng) {

        String startId = nearestNodeId(lat, lng);
        List<Node> listBLNodes = nodes.findByBlueLight(true);
        Set<Node> blNodes = new HashSet<>(listBLNodes);
        
    
        if(blNodes.size() == 0){
            return null;
        }
        System.out.println("StartID:"+startId+"\n");
        System.out.println("BlueLight Size:"+String.valueOf(blNodes.size())+"\n");


        
        Map<String, String> path = Astar(startId, blNodes, -1l,true);
        System.out.println("*****************************************");
        String cur = "";
        for (Node curNode : listBLNodes) {
            System.out.println("checking:  "+curNode.getId());
            if (path.keySet().contains(curNode.getId())) {
                cur = curNode.getId();
                System.out.println("Found end :  "+cur);
            }
        }
        ResponseObjectNavigateBlueLight resp = new ResponseObjectNavigateBlueLight();
        resp.dest = cur;
        Set<String> pathEdges = new HashSet<>();
        while (path.get(cur) != null) {
            String nxt = path.get(cur);

            if (edges.findByFromNodeAndToNode(cur, nxt)!=null) {
                pathEdges.add(edges.findByFromNodeAndToNode(cur, nxt).getKey());
            } else if (edges.findByFromNodeAndToNode(nxt, cur)!=null) {
                pathEdges.add(edges.findByFromNodeAndToNode(nxt, cur).getKey());
            }
            cur = nxt;
        }
        resp.path = pathEdges;
        return resp;
 
    }
    public Set<String> navigate(double lat, double lng, String DestinationId, Long navModeId) {

        String startId = nearestNodeId(lat, lng);
        NavMode pathNavMode = navr.findById(navModeId).get();
        Set<Node> destinationNodes = building.findById(DestinationId).get().getNodes();

        System.out.println("StartID:"+startId);
        System.out.println("NavMode :"+pathNavMode.getName());
        System.out.println("Destination :"+destinationNodes.size());

        Map<String, String> path = Astar(startId, destinationNodes, navModeId,false);
        System.out.println("************");
        System.out.println(path);
        System.out.println("************");

        String cur = "";
        for (Node curNode : destinationNodes) {
            if (path.keySet().contains(curNode.getId())) {
                cur = curNode.getId();
            }
        }

        Set<String> pathEdges = new HashSet<>();
        int j = 0;
        for (int i = 0; i < path.size() ; i += 1) {
            String nxt = path.get(cur);

            if (edges.findByFromNodeAndToNode(cur, nxt) != null) {
                if(!edges.findByFromNodeAndToNode(cur, nxt).isInterBuildingEdge()){
                pathEdges.add(edges.findByFromNodeAndToNode(cur, nxt).getKey());
                }
                j+=1;
            } else if (edges.findByFromNodeAndToNode(nxt, cur)!= null) {
                if(!edges.findByFromNodeAndToNode(nxt, cur).isInterBuildingEdge()){
                pathEdges.add(edges.findByFromNodeAndToNode(nxt, cur).getKey());
                }
                j+=1;
            } else {
                System.out.println("OOOOHhh NNNOOO!");
            }
            cur = nxt;
        }
        System.out.println("j:"+String.valueOf(j));
                return pathEdges;
    }

    private Map<String, String> Astar(String start, Set<Node> end, Long pathNavModeId, boolean anyNavMode) {

        Double endLat = 0.0;
        Double endLng = 0.0;
        Double N = 0.0;
        Set<String> ends = new HashSet<>();
        for (Node cur : end) {
            N += 1;
            endLat += cur.getLat();
            endLng += cur.getLng();
            ends.add(cur.getId());
        }
        if (ends.contains(start)) {
            return null;
        }

        endLat /= N;
        endLng /= N;

        // f = g + h
        record State(String id, double f) {
        }
        PriorityQueue<State> open = new PriorityQueue<>(Comparator.comparingDouble(State::f));
        Map<String, Double> g = new HashMap<>();
        Map<String, String> parent = new HashMap<>();
        Set<String> closed = new HashSet<>();

        g.put(start, 0.0);
        open.add(new State(start, heuristic(start, endLat, endLng)));

        while (!open.isEmpty()) {
            String cur = open.poll().id();
            if (!closed.add(cur))
                continue; // skip if already expanded
            if (ends.contains(cur)) {
                // System.out.println("found the end");
                // System.out.println("found the end");
                // System.out.println(cur);
                return parent;
            }

            for (Adj e : neighbors(cur)) {
                // System.out.println(e.to());
                String nxt = e.to(); // Adj(to, distance)
                if (closed.contains(nxt))
                    continue;

                double tentative = g.get(cur) + e.distance();
                if ((tentative < g.getOrDefault(nxt, Double.POSITIVE_INFINITY)) && (e.navModes.contains(pathNavModeId)|| e.fromThrough || anyNavMode)) {
                    g.put(nxt, tentative);
                    parent.put(nxt, cur);
                    double f = tentative + heuristic(nxt, endLat, endLng);
                    open.add(new State(nxt, f));
                }
            }
        }

        return null;
    }

    private double heuristic(String cur, Double endLat, Double endLng) {
        double start_ll[] = nodeCoords.get(cur);

        double result = Math.sqrt(Math.pow(start_ll[0] - endLat, 2) +
                Math.pow(start_ll[1] - endLng, 2));
        return result;
    }

    // distane in meters
    public static Double calDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
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

    public static Double deg2rad(Double deg) {
        return deg * (Math.PI / 180);
    }
}
