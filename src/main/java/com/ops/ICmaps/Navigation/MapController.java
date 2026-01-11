package com.ops.ICmaps.Navigation;

import java.util.List;
import java.util.Set;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ops.ICmaps.Edge.Edge;
import com.ops.ICmaps.Edge.EdgeRepository;
import com.ops.ICmaps.NavMode.NavMode;
import com.ops.ICmaps.NavMode.NavModeRepository;
import com.ops.ICmaps.Navigation.GraphService.ResponseObjectNavigateBlueLight;
import static com.ops.ICmaps.Navigation.GraphService.calDistance;
import com.ops.ICmaps.Node.Node;
import com.ops.ICmaps.Node.NodeRepository;

@RestController
@CrossOrigin
@RequestMapping("/map")
public class MapController {

  private final NodeRepository nr;
  private final EdgeRepository er;
  private final ObjectMapper objectMapper;
  private final GraphService gs;
  private final NavModeRepository navr;

  public MapController(NavModeRepository navr, GraphService gs, EdgeRepository er, NodeRepository nr,
      ObjectMapper objectMapper) {
    this.nr = nr;
    this.er = er;
    this.gs = gs;
    this.navr = navr;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/allpedes")
  public ObjectNode MakeAllPedes() {
    ObjectNode objectNode = objectMapper.createObjectNode();
    Long c = 1L;
    NavMode curnavMode = navr.findById(c).get();

    List<Node> nodes = nr.findAll();
    for (Node cur : nodes) {
      if (!cur.getNavModes().contains(curnavMode)) {
        cur.AddNavMode(curnavMode);
      }
    }
    nr.saveAll(nodes);
    List<Edge> edges = er.findAll();
    for (Edge cur : edges) {
      if (!cur.getNavModes().contains(curnavMode)) {
        cur.AddNavMode(curnavMode);
      }
    }

    er.saveAll(edges);
    nr.saveAll(nodes);
    navr.save(curnavMode);
    gs.loadGraph();
    objectNode.put("message", "Found it!");
    return objectNode;
  }

  @GetMapping("/")
  public ObjectNode HealthCheck() {
    ObjectNode objectNode = objectMapper.createObjectNode();
    objectNode.put("message", "Found it!");
    return objectNode;
  }

  @GetMapping("/navigateTo")
  public ObjectNode RouteTo(@RequestParam String id,
      @RequestParam Long navMode,
      @RequestParam Double lat,
      @RequestParam Double lng) {
    ObjectNode objectNode = objectMapper.createObjectNode();

    System.out.println(lat + " " + lng + " " + navMode + " " + id);
    System.out.println("everything fine here");
    Set<String> path = gs.navigate(lat, lng, id, navMode);
    for (String no : path) {
      System.out.println(no);
    }
    objectNode.set("path", objectMapper.valueToTree(path));
    return objectNode;
  }

  public record EdgeDTO(String key, String from, String to, double distance, boolean biDirectional) {

  }

  public record NodeDTO(String id, double lat, double lng, boolean isBlueLight) {

  }

  @GetMapping("/all")
  public ObjectNode getAllFeatures() {

    ObjectNode objectNode = objectMapper.createObjectNode();
    List<Node> nodes = nr.findAll();
    List<Edge> edges = er.findByInterBuildingEdge(false);
    List<NodeDTO> nodeDTOs = nodes.stream()
        .map(e -> new NodeDTO(
            e.getId(),
            e.getLat(),
            e.getLng(),
            e.isBlueLight()))
        .toList();
    List<EdgeDTO> edgeDTOs = edges.stream()
        .map(e -> new EdgeDTO(
            e.getKey(),
            e.getFromNode(),
            e.getToNode(),
            e.getDistance(),
            e.isBiDirectional()))
        .toList();

    objectNode.set("nodes", objectMapper.valueToTree(nodeDTOs));
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
      gs.loadGraph();
    } else if (type.equals("edge")) {
      String key = args.get("key").asText();
      String from = args.get("from").asText();
      String to = args.get("to").asText();
      boolean biDirectional = args.get("biDirectional").asBoolean();

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
      er.save(new Edge(distance, to, from, key, biDirectional, false, ""));
      objectNode.put("message", "Edge added!");
      gs.loadGraph();
    } else {
      System.out.println("feature not recognized!");
      objectNode.put("message", "Feature not recognized!");
    }

    return objectNode;
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
    gs.loadGraph();
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
      gs.loadGraph();
    } else if (type.equals("edge")) {
      Edge curEdge = er.findById(featureKey).get();
      er.delete(curEdge);
      objectNode.put("message", "Edge deleted!");
      gs.loadGraph();
    } else {
      System.out.println("feature not recognized!");
      objectNode.put("message", "Feature not recognized!");
    }
    return objectNode;
  }

  @PostMapping("/bluelight")
  public ObjectNode SetBlueLight(@RequestBody ObjectNode args) {
    ObjectNode objectNode = objectMapper.createObjectNode();
    String nodeId = args.get("nodeId").asText();
    boolean blueLight = args.get("isBlueLight").asBoolean();
    System.out.println(blueLight);
    Node curNode = nr.findById(nodeId).get();
    curNode.setBlueLight(blueLight);
    nr.save(curNode);
    objectNode.put("message", "Blue light status updated!");
    gs.loadGraph();
    return objectNode;
  }

  @GetMapping("/bluelight")
  public ObjectNode ERouteTo(@RequestParam Double lat, @RequestParam Double lng) {
    ObjectNode objectNode = objectMapper.createObjectNode();
    ResponseObjectNavigateBlueLight resp = gs.navigateBlueLight(lat, lng);
    objectNode.set("path", objectMapper.valueToTree(resp.getPath()));
    objectNode.put("dest", resp.getDest());
    gs.loadGraph();
    return objectNode;
  }

}
