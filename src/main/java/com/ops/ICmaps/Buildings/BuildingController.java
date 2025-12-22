package com.ops.ICmaps.Buildings;

import java.util.List;
import java.util.Set;

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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ops.ICmaps.Edge.Edge;
import com.ops.ICmaps.Edge.EdgeRepository;
import com.ops.ICmaps.Navigation.GraphService;
import static com.ops.ICmaps.Navigation.GraphService.calDistance;
import com.ops.ICmaps.Node.Node;
import com.ops.ICmaps.Node.NodeRepository;

@RestController
@CrossOrigin
@RequestMapping("/building")
public class BuildingController {

    private final NodeRepository nr;
    private final EdgeRepository er;
    private final BuildingRepository br;
    private final GraphService gs;
    private final ObjectMapper objectMapper;

    public BuildingController(GraphService gs,EdgeRepository er,BuildingRepository br, NodeRepository nr, ObjectMapper objectMapper) {
        this.nr = nr;
        this.br = br;
        this.er = er;
        this.gs = gs;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/")
    @ResponseBody
    public ObjectNode AddBuilding(@RequestBody Building newBuilding) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        try {
            br.save(newBuilding);
            objectNode.put("message", "Building added!");
        } catch (Exception e) {
            objectNode.put("message", e.toString());
        }
        return objectNode;
    }

    @PutMapping("/")
    @ResponseBody
    public ObjectNode EditBuilding(@RequestBody Building newBuilding) {
        ObjectNode objectNode = objectMapper.createObjectNode();

        String respMessage = br.findById(newBuilding.getId()).map(curBuilding -> {
            curBuilding.setName(newBuilding.getName());
            br.save(curBuilding);
            return "Building name update!";
        }).orElseGet(() -> {
            return "Building not found!";
        });
        objectNode.put("message", respMessage);
        return objectNode;
    }

    @PatchMapping("/setpolygon")
    @ResponseBody
    public ObjectNode SetBuildingPolyGon(@RequestBody ObjectNode args) {
        ObjectNode objectNode = objectMapper.createObjectNode();

        String buildingId = args.get("buildingId").asText();
        Double lat = args.get("lat").asDouble();
        Double lng = args.get("lng").asDouble();
        String polyGon = args.get("polygonJson").asText();

        try{
            Building curBuilding = br.findById(buildingId).get();
            curBuilding.setLat(lat);
            curBuilding.setLng(lng);
            curBuilding.setPolyGon(polyGon);
            br.save(curBuilding);
            objectNode.put("message", "PolyGon Updated!");
        }catch(Exception e){
            objectNode.put("message", e.toString());
        }

        
        return objectNode;
    }

    @DeleteMapping("/setpolygon")
    @ResponseBody
    public ObjectNode RemoveBuildingPolyGon(@RequestBody ObjectNode args) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        String buildingId = args.get("buildingId").asText();
        try{
            Building curBuilding = br.findById(buildingId).get();
            curBuilding.setPolyGon("");
            br.save(curBuilding);
            objectNode.put("message", "PolyGon Updated!");
        }catch(Exception e){
            objectNode.put("message", e.toString());
        }

        
        return objectNode;
    }

    @DeleteMapping("/")
    @ResponseBody
    public ObjectNode DeleteBuilding(@RequestBody ObjectNode args) {
        ObjectNode objectNode = objectMapper.createObjectNode();

        String buildingId = args.get("id").asText();

        try {
            br.deleteById(buildingId);
            objectNode.put("message", "Building deleted!");
        } catch (Exception e) {
            objectNode.put("message", e.toString());
        }

        return objectNode;
    }

    public record BuildingsDTO(String id, String name, double lat, double lng,String polyGon) {

    }

    @GetMapping("/")
    public ObjectNode GetAllBuildings() {
        ObjectNode objectNode = objectMapper.createObjectNode();
        List<Building> allBuildings = br.findAll();
        List<BuildingsDTO> NavmodeDTOs = allBuildings.stream()
                .map(e -> new BuildingsDTO(
                        e.getId(),
                        e.getName(),
                        e.getLat(),
                        e.getLng(),
                    e.getPolyGon()))
                .toList();

        objectNode.set("buildings", objectMapper.valueToTree(NavmodeDTOs));
        return objectNode;
    }
public record NodeDtoo(String id, double lat, double lng) {}



    @GetMapping("/nodesget")
    public ObjectNode GetAllBuildingNodes(@RequestParam String id) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        Building building = br.findById(id).get();
        Set<Node> Nodes =  building.getNodes();
        
        List<NodeDtoo> nodeDTOs = Nodes.stream()
                .map(e -> new NodeDtoo(
                        e.getId(),
                        e.getLat(),
                        e.getLng()
                    ))
                .toList();
      
        objectNode.set("nodes", objectMapper.valueToTree(nodeDTOs));
        return objectNode;
    }

    @GetMapping("/buildingpos")
    public ObjectNode GetBuildingPos(@RequestParam String id) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        Building building = br.findById(id).get();
        BuildingsDTO re = new BuildingsDTO(
                        building.getId(),
                        building.getName(),
                        building.getLat(),
                        building.getLng(),
                   building.getPolyGon());
        // building DTO (safe)
    objectNode.set("building", objectMapper.valueToTree(re));

    // node list as lightweight JSON to avoid cycles
    ArrayNode nodesArray = objectNode.putArray("building_nodes");
    for (Node n : building.getNodes()) {
        ObjectNode nNode = objectMapper.createObjectNode();
        nNode.put("id", n.getId());
        nNode.put("lat", n.getLat());
        nNode.put("lng", n.getLng());
        // add only what the frontend needs
        nodesArray.add(nNode);
    }


	
        return objectNode;
    }

    @PostMapping("/nodeadd")
    public ObjectNode NodeAdd(@RequestBody ObjectNode args) {

        ObjectNode objectNode = objectMapper.createObjectNode();
        String buildingId = args.get("buildingId").asText();
        String nodeId = args.get("nodeId").asText();

        Building curBuilding = br.findById(buildingId).get();
        Set<Node> curBuildingNodes = curBuilding.getNodes();
        Node curNode = nr.findById(nodeId).get();
        
        for(Node curBuildingNode: curBuildingNodes){
            double distance = calDistance(curNode.getLat(), curNode.getLng(), curBuildingNode.getLat(), curBuildingNode.getLng());
            Edge newEdge = new Edge(
                                distance,
                                curNode.getId(),
                                curBuildingNode.getId(),
                                curNode.getId()+"__"+curBuildingNode.getId(),
                                true,
                                true,
                                curBuilding.getId());
                        er.save(newEdge);
        }

        curBuilding.addNode(curNode);
        nr.save(curNode);
        br.save(curBuilding);
        gs.loadGraph();
        return objectNode;
    }

    @PostMapping("/noderemove")
    public ObjectNode RemoveNode(@RequestBody ObjectNode args) {

        ObjectNode objectNode = objectMapper.createObjectNode();
        String buildingId = args.get("buildingId").asText();
        String nodeId = args.get("nodeId").asText();

        Building curBuilding = br.findById(buildingId).get();
        Node curNode = nr.findById(nodeId).get();
        List<Edge> interBuildingEdges = er.findByBuildingId(buildingId);

        for(Edge curEdge: interBuildingEdges){
            er.delete(curEdge);
        }

        curBuilding.removeNode(curNode);
        nr.save(curNode);
        br.save(curBuilding);
        gs.loadGraph();
        return objectNode;
    }
}
