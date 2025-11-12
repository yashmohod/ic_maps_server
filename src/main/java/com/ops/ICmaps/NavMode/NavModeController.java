package com.ops.ICmaps.NavMode;

import java.util.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ops.ICmaps.Buildings.Building;
import com.ops.ICmaps.Buildings.BuildingRepository;
import com.ops.ICmaps.Edge.Edge;
import com.ops.ICmaps.Edge.EdgeRepository;
import com.ops.ICmaps.Node.Node;
import com.ops.ICmaps.Node.NodeRepository;

@RestController
@CrossOrigin
@RequestMapping("/navmode")
public class NavModeController {

    private final NodeRepository nr;
    private final EdgeRepository er;
    private final NavModeRepository navr;
    private final ObjectMapper objectMapper;

    public NavModeController(NavModeRepository navr, EdgeRepository er, NodeRepository nr, ObjectMapper objectMapper) {
        this.nr = nr;
        this.er = er;
        this.navr = navr;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/")
    public ObjectNode AddNavMode(@RequestBody NavMode newNavMode) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        try {
            System.out.println(newNavMode.getName());
            navr.save(newNavMode);
            objectNode.put("message", "NavMode added!");
        } catch (Exception e) {
            objectNode.put("message", e.toString());
        }
        return objectNode;
    }

    @PutMapping("/")
    @ResponseBody
    public ObjectNode EditNavMode(@RequestBody NavMode newNavMode) {
        ObjectNode objectNode = objectMapper.createObjectNode();

        String respMessage = navr.findById(newNavMode.getId()).map(curNavMode -> {
            curNavMode.setName(newNavMode.getName());
            navr.save(curNavMode);
            return "NavMode name update!";
        }).orElseGet(() -> {
            return "NavMode not found!";
        });
        objectNode.put("message", respMessage);
        return objectNode;
    }

    @DeleteMapping("/")
    @ResponseBody
    public ObjectNode DeleteNavMode(@RequestBody ObjectNode args) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        Long navModeId = args.get("id").asLong();
        try {
            navr.deleteById(navModeId);
            objectNode.put("message", "NavMode deleted!");
        } catch (Exception e) {
            objectNode.put("message", e.toString());
        }
        return objectNode;
    }

    @GetMapping("/")
    public ObjectNode GetAllNavModes() {
        ObjectNode objectNode = objectMapper.createObjectNode();
        List<NavMode> allNavModes = navr.findAll();
        objectNode.set("NavModes", objectMapper.valueToTree(allNavModes));
        return objectNode;
    }

    @PatchMapping("/setstatus")
    @ResponseBody
    public ObjectNode NavModeSetStatus(@RequestBody ObjectNode args) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        String type = args.get("featureType").asText();
        Long navModeId = args.get("navModeId").asLong();
        NavMode curNavMode = navr.findById(navModeId).get();
        Boolean value = args.get("value").asBoolean();
        System.out.println(type + " " + navModeId + " " + curNavMode.getId() + " " + value);
        if (type.equals("Node")) {
            Node curNode = nr.findById(args.get("id").asText()).get();
            System.out.println("Contains: " + curNavMode.containsNode(curNode));
            if (value) {
                if (!curNavMode.containsNode(curNode)) {
                    try {
                        curNavMode.addNode(curNode);
                        nr.save(curNode);
                        navr.save(curNavMode);
                    } catch (Exception e) {
                        objectNode.put("message", e.toString());
                    }

                }
            } else {
                if (curNavMode.containsNode(curNode)) {
                    try {
                        curNavMode.removeNode(curNode);
                        nr.save(curNode);
                        navr.save(curNavMode);

                    } catch (Exception e) {
                        objectNode.put("message", e.toString());
                    }
                }
            }

        } else if (type.equals("Edge")) {
            Edge curEdge = er.findById(args.get("id").asText()).get();
            if (value) {
                if (!curNavMode.containsEdge(curEdge)) {
                    try {
                        curNavMode.addEdge(curEdge);
                        navr.save(curNavMode);
                        er.save(curEdge);
                    } catch (Exception e) {
                        objectNode.put("message", e.toString());
                    }
                }
            } else {
                if (curNavMode.containsEdge(curEdge)) {
                    try {
                        curNavMode.removeEdge(curEdge);
                        navr.save(curNavMode);
                        er.save(curEdge);
                    } catch (Exception e) {
                        objectNode.put("message", e.toString());
                    }
                }
            }

        } else {
            objectNode.put("message", "Feature type not recognized!");
        }

        return objectNode;
    }

    @GetMapping("/allids")
    @ResponseBody
    public ObjectNode GetAllFeatureIds(@RequestParam Long navModeId) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        NavMode curNavMode = navr.findById(navModeId).get();

        Set<Node> nodes = curNavMode.getNodes();
        List<String> nodeList = new ArrayList<String>();

        for (Node curNode : nodes) {
            nodeList.add(curNode.getId());
        }

        Set<Edge> edges = curNavMode.getEdges();
        List<String> edgeList = new ArrayList<String>();

        for (Edge curNode : edges) {
            edgeList.add(curNode.getKey());
        }

        objectNode.set("nodes", objectMapper.valueToTree(nodeList));
        objectNode.set("edges", objectMapper.valueToTree(edgeList));
        return objectNode;
    }

    @GetMapping("/all")
    public ObjectNode GetAllFeatures(@RequestParam Long navModeId) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        NavMode curNavMode = navr.findById(navModeId).get();
        System.out.println("here is the nav mode id :" + curNavMode);
        Set<Node> nodes = curNavMode.getNodes();
        Set<Edge> edges = curNavMode.getEdges();

        objectNode.set("nodes", objectMapper.valueToTree(nodes));
        objectNode.set("edges", objectMapper.valueToTree(edges));
        return objectNode;
    }

}
