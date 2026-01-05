package com.ops.ICmaps;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ops.ICmaps.Buildings.BuildingRepository;
import com.ops.ICmaps.Edge.Edge;
import com.ops.ICmaps.Edge.EdgeRepository;
import com.ops.ICmaps.NavMode.NavMode;
import com.ops.ICmaps.NavMode.NavModeRepository;
import com.ops.ICmaps.Navigation.GraphService;
import com.ops.ICmaps.Node.Node;
import com.ops.ICmaps.Node.NodeRepository;

@Configuration
class LoadDatabase {

    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

    private final NodeRepository nr;
    private final EdgeRepository er;
    private final NavModeRepository navr;
    private final GraphService gs;
    private final BuildingRepository br;

    public LoadDatabase(BuildingRepository br, GraphService gs, NavModeRepository navr, EdgeRepository er,
            NodeRepository nr) {
        this.nr = nr;
        this.er = er;
        this.gs = gs;
        this.br = br;
        this.navr = navr;
    }

    @Bean
    CommandLineRunner initDatabase(
            @Value("classpath:phaseOne.json") org.springframework.core.io.Resource json) {
        return args -> {

            JSONParser jp = new JSONParser();

            try (InputStreamReader reader = new InputStreamReader(json.getInputStream(), StandardCharsets.UTF_8)) {

                String[] navModes = { "Pedestrian", "ADA", "Vehicular" };

                for (String mode : navModes) {
                    if (!navr.existsNavModeByName(mode)) {
                        NavMode curMode = new NavMode(null, mode,false);
                        navr.save(curMode);
                    }
                }

                JSONObject onj = (JSONObject) jp.parse(reader);
                JSONArray features = (JSONArray) onj.get("features");

                for (Object elem : features) {
                    JSONObject feature = (JSONObject) elem;
                    JSONObject geometry = (JSONObject) feature.get("geometry");
                    String type = (String) geometry.get("type");
                    JSONArray cords = (JSONArray) geometry.get("coordinates");
                    JSONObject prop = (JSONObject) feature.get("properties");
                    // System.out.println(type);

                    if ("Point".equals(type) && !nr.existsById((String) prop.get("id"))) {

                        Node newNode = new Node(
                                (String) prop.get("id"),
                                (Double) cords.get(0),
                                (Double) cords.get(1));
                        nr.save(newNode);
                    }

                    if ("LineString".equals(type) && !er.existsById((String) prop.get("key"))) {

                        String key = (String) prop.get("key");
                        String fromId = (String) prop.get("from");
                        String toId = (String) prop.get("to");

                        JSONArray from = (JSONArray) cords.get(0);
                        JSONArray to = (JSONArray) cords.get(1);

                        Double latFrom = (Double) from.get(0);
                        Double lngFrom = (Double) from.get(1);
                        Double latTo = (Double) to.get(0);
                        Double lngTo = (Double) to.get(1);

                        Double distance = gs.calDistance(latFrom, lngFrom, latTo, lngTo);

                        Edge newEdge = new Edge(
                                distance,
                                fromId,
                                toId,
                                key,
                                true,
                                false,
                                "");
                        er.save(newEdge);
                    }
                }

                log.info("Ran - Preloading data from phaseOne.json");

            } catch (Exception e) {
                log.error("Failed to load phaseOne.json", e);
            }
        };
    }

}
