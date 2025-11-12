package com.ops.ICmaps.Navigation;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.ops.ICmaps.Edge.Edge;
import com.ops.ICmaps.Edge.EdgeRepository;
import com.ops.ICmaps.Navigation.GraphService.Adj;
import com.ops.ICmaps.Node.Node;
import com.ops.ICmaps.Node.NodeRepository;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@CrossOrigin
@RequestMapping("/map")
public class MapController {

    private final NodeRepository nr;
    private final EdgeRepository er;
    private final ObjectMapper objectMapper;
    private final GraphService gs;

    public MapController(GraphService gs, EdgeRepository er, NodeRepository nr, ObjectMapper objectMapper) {
        this.nr = nr;
        this.er = er;
        this.gs = gs;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public ObjectNode HealthCheck() {
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("message", "Found it!");
        return objectNode;
    }

    @GetMapping("/routeto")
    public ObjectNode Route(ObjectNode args) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        double lat = args.get("lat").asDouble();
        double lng = args.get("lng").asDouble();
        Long navModeId = args.get("navMode").asLong();
        Long buildingId = args.get("navMode").asLong();

        String[] path = gs.navigate(lat, lng, buildingId, navModeId);
        objectNode.set("path", objectMapper.valueToTree(path));
        return objectNode;
    }

    public record EdgeDTO(String key, String from, String to, double distance) {

    }

    @GetMapping("/all")
    public ObjectNode getAllNodes() {

        ObjectNode objectNode = objectMapper.createObjectNode();
        List<Node> nodes = nr.findAll();
        List<Edge> edges = er.findAll();

        List<EdgeDTO> edgeDTOs = edges.stream()
                .map(e -> new EdgeDTO(
                        e.getKey(),
                        e.getFromNode(),
                        e.getToNode(),
                        e.getDistance()))
                .toList();

        objectNode.set("nodes", objectMapper.valueToTree(nodes));
        objectNode.set("edges", objectMapper.valueToTree(edgeDTOs));
        return objectNode;
    }

    // feature CRUD
    @PostMapping("/")
    @ResponseBody
    public ObjectNode AddFeature(@RequestBody ObjectNode args) {
        ObjectNode objectNode = objectMapper.createObjectNode();

        String type = args.get("type").asText();

        if (type.equals("node")) {
            String id = args.get("id").asText();
            Double lat = args.get("lat").asDouble();
            Double lng = args.get("lng").asDouble();

            nr.save(new Node(id, lng, lat));
            objectNode.put("message", "Node added!");

        } else if (type.equals("edge")) {
            String key = args.get("key").asText();
            String from = args.get("from").asText();
            String to = args.get("to").asText();

            Double[] FromCords = nr.findById(from).map(foundNode -> {
                Double[] Foundlatlng = { foundNode.getLat(), foundNode.getLng() };
                return Foundlatlng;
            }).orElseGet(() -> {
                Double[] Foundlatlng = { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
                return Foundlatlng;

            });

            Double[] ToCords = nr.findById(to).map(foundNode -> {
                Double[] Foundlatlng = { foundNode.getLat(), foundNode.getLng() };
                return Foundlatlng;
            }).orElseGet(() -> {
                Double[] Foundlatlng = { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
                return Foundlatlng;

            });

            if ((FromCords[0] == Double.NEGATIVE_INFINITY && FromCords[1] == Double.NEGATIVE_INFINITY)
                    || (ToCords[0] == Double.NEGATIVE_INFINITY && ToCords[1] == Double.NEGATIVE_INFINITY)) {
                objectNode.put("message", "Nodes not found!");
                return objectNode;
            }

            double distance = calDistance(FromCords[0], FromCords[1], ToCords[0], ToCords[1]);
            System.out.println(distance);
            er.save(new Edge(distance, to, from, key));
            objectNode.put("message", "Edge added!");

        } else {
            System.out.println("feature not recognized!");
            objectNode.put("message", "Feature not recognized!");
        }

        return objectNode;
    }

    // distane in meters
    Double calDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        Double R = 6371.0; // Radius of the earth in km
        Double dLat = deg2rad(lat2 - lat1); // deg2rad below
        Double dLon = deg2rad(lon2 - lon1);
        Double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        Double d = R * c; // Distance in km
        return d * 1000;
    }

    Double deg2rad(Double deg) {
        return deg * (Math.PI / 180);
    }

    @PutMapping("/")
    public ObjectNode UpdateNode(@RequestBody Node node) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        System.out.println(node.getId());
        Node existingNode;
        try {
            existingNode = nr.findById(node.getId()).get();
            existingNode.setLat(node.getLat());
            existingNode.setLng(node.getLng());

        } catch (Exception e) {
            objectNode.put("message", e.toString());
            return objectNode;
        }

        List<Edge> toEdges = er.findByFromNode(node.getId());

        for (Edge curEdge : toEdges) {
            Node toNode = nr.getReferenceById(curEdge.getToNode());
            double distance = calDistance(node.getLat(), node.getLng(), toNode.getLat(), toNode.getLng());
            curEdge.setDistance(distance);
        }

        List<Edge> fromEdges = er.findByToNode(node.getId());

        for (Edge curEdge : fromEdges) {
            Node fromNode = nr.getReferenceById(curEdge.getFromNode());
            double distance = calDistance(node.getLat(), node.getLng(), fromNode.getLat(), fromNode.getLng());
            curEdge.setDistance(distance);
        }

        er.saveAll(fromEdges);
        er.saveAll(toEdges);
        nr.save(existingNode);
        objectNode.put("message", "Node updated!");
        return objectNode;
    }

    @DeleteMapping("/")
    public ObjectNode DeleteFeature(@RequestBody ObjectNode args) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        String type = args.get("featureType").asText();
        String featureKey = args.get("featureKey").asText();
        System.out.println(type + " " + featureKey);

        if (type.equals("node")) {
            Node curNode = nr.findById(featureKey).get();
            List<Edge> toEdges = er.findByFromNode(curNode.getId());
            List<Edge> fromEdges = er.findByToNode(curNode.getId());
            er.deleteAll(toEdges);
            er.deleteAll(fromEdges);
            nr.delete(curNode);
            objectNode.put("message", "Node and its edges deleted!");
        } else if (type.equals("edge")) {
            Edge curEdge = er.findById(featureKey).get();
            er.delete(curEdge);
            objectNode.put("message", "Edge deleted!");
        } else {
            System.out.println("feature not recognized!");
            objectNode.put("message", "Feature not recognized!");
        }
        return objectNode;
    }

}
