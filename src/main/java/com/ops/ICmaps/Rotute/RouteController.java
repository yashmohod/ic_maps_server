package com.ops.ICmaps.Rotute;

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
import com.ops.ICmaps.Buildings.Building;
import com.ops.ICmaps.Buildings.BuildingRepository;
import com.ops.ICmaps.Navigation.GraphService;

@RestController
@CrossOrigin
@RequestMapping("/shareableroute")
public class RouteController {

  private final RouteRepository rr;
  private final ObjectMapper objectMapper;
  private final BuildingRepository br;
  private final GraphService gr;

  public RouteController(RouteRepository rr, ObjectMapper objectMapper, BuildingRepository br, GraphService gr) {
    this.rr = rr;
    this.br = br;
    this.gr = gr;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/")
  @ResponseBody
  public ObjectNode GetRoute(@RequestBody Long routeId) {
    ObjectNode objectNode = objectMapper.createObjectNode();
    Route curRoute = rr.findById(routeId).get();
    objectNode.set("route", objectMapper.valueToTree(curRoute));
    return objectNode;
  }

  public record RouteDTO(Long id, String destinationId, String destinationName, String name) {
  }

  @GetMapping("/all")
  public ObjectNode getRoutes(@RequestParam String userId) {
    ObjectNode objectNode = objectMapper.createObjectNode();

    List<Route> userRoutes = rr.findByUserId(userId);

    List<RouteDTO> routeDTOs = userRoutes.stream()
        .map(
            r -> new RouteDTO(
                r.getId(), r.getDestinationId(), r.getDestinationName(), r.getName()))
        .toList();

    objectNode.set("routes", objectMapper.valueToTree(routeDTOs));
    return objectNode;
  }

  @PostMapping("/")
  @ResponseBody
  public ObjectNode addRoute(@RequestBody ObjectNode args) {
    ObjectNode objectNode = objectMapper.createObjectNode();

    try {
      String userId = args.get("userId").asText();
      String routeName = args.get("routeName").asText();
      String destinationId = args.get("destinationId").asText();

      Building destination = br.findById(destinationId)
          .orElseThrow(() -> new IllegalArgumentException("Destination not found"));
      System.out.println(
          " userId: "
              + userId
              + " routeName: "
              + routeName
              + " destinationId: "
              + destinationId
              + " destinationName: "
              + destination.getName());
      Route newRoute = new Route(userId, destinationId, destination.getName(), routeName);

      Route saved = rr.save(newRoute); // <-- id assigned here by DB/JPA

      objectNode.put("message", "Route added!");
      objectNode.put("id", saved.getId()); // return it to the frontend
    } catch (Exception e) {
      objectNode.put("message", e.toString());
    }

    return objectNode;
  }

  @PutMapping("/")
  @ResponseBody
  public ObjectNode EditRoute(@RequestBody ObjectNode args) {
    ObjectNode objectNode = objectMapper.createObjectNode();

    try {
      Long routeId = args.get("routeId").asLong();
      String routeName = args.get("routeName").asText();
      String destinationId = args.get("destinationId").asText();
      Building destination = br.findById(destinationId)
          .orElseThrow(() -> new IllegalArgumentException("Destination not found"));

      Route curRoute = rr.findById(routeId).get();

      curRoute.setDestinationId(destinationId);
      curRoute.setDestinationName(destination.getName());
      curRoute.setName(routeName);

      Route saved = rr.save(curRoute); // <-- id assigned here by DB/JPA

      objectNode.put("message", "Route added!");
      objectNode.put("id", saved.getId()); // return it to the frontend
    } catch (Exception e) {
      objectNode.put("message", e.toString());
    }

    return objectNode;
  }

  @DeleteMapping("/")
  @ResponseBody
  public ObjectNode DeleteRoute(@RequestBody ObjectNode args) {
    ObjectNode objectNode = objectMapper.createObjectNode();

    try {
      Long routeId = args.get("routeId").asLong();
      rr.deleteById(routeId);
      objectNode.put("message", "Route deleted!");
    } catch (Exception e) {
      objectNode.put("message", e.toString());
    }
    return objectNode;
  }

  @GetMapping("/navigate")
  public ObjectNode RouteTo(@RequestParam Long routeId,
      @RequestParam Long navModeId,
      @RequestParam Double userLat,
      @RequestParam Double userLng) {
    ObjectNode objectNode = objectMapper.createObjectNode();
    Route curRoute = rr.findById(routeId).get();
    Set<String> path = gr.navigate(userLat, userLng, curRoute.getDestinationId(), navModeId);

    objectNode.set("path", objectMapper.valueToTree(path));
    return objectNode;
  }
}
