package com.ops.ICmaps;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ops.ICmaps.Edge.Edge;
import com.ops.ICmaps.Edge.EdgeRepository;
import com.ops.ICmaps.NavMode.NavMode;
import com.ops.ICmaps.NavMode.NavModeRepository;
import com.ops.ICmaps.Node.Node;
import com.ops.ICmaps.Node.NodeRepository;

@Configuration
class LoadDatabase {

    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

    private final NodeRepository nr;
    private final EdgeRepository er;
    private final NavModeRepository navr;
    private final ObjectMapper objectMapper;

    public LoadDatabase(NavModeRepository navr, EdgeRepository er, NodeRepository nr, ObjectMapper objectMapper) {
        this.nr = nr;
        this.er = er;
        this.navr = navr;
        this.objectMapper = objectMapper;
    }

    @Bean
    CommandLineRunner initDatabase(
            @Value("classpath:phaseOne.json") org.springframework.core.io.Resource json) {
        return args -> {

            JSONParser jp = new JSONParser();

            try (InputStreamReader reader = new InputStreamReader(json.getInputStream(), StandardCharsets.UTF_8)) {

                JSONObject onj = (JSONObject) jp.parse(reader);
                JSONArray features = (JSONArray) onj.get("features");

                for (Object elem : features) {
                    JSONObject feature = (JSONObject) elem;
                    JSONObject geometry = (JSONObject) feature.get("geometry");
                    String type = (String) geometry.get("type");
                    JSONArray cords = (JSONArray) geometry.get("coordinates");
                    JSONObject prop = (JSONObject) feature.get("properties");

                    if ("Point".equals(type)) {

                        Node newNode = new Node(
                                (String) prop.get("id"),
                                (Double) cords.get(0),
                                (Double) cords.get(1));
                        nr.save(newNode);
                    }

                    if ("LineString".equals(type)) {

                        String key = (String) prop.get("key");
                        String fromId = (String) prop.get("from");
                        String toId = (String) prop.get("to");

                        JSONArray from = (JSONArray) cords.get(0);
                        JSONArray to = (JSONArray) cords.get(1);

                        Double latFrom = (Double) from.get(0);
                        Double lngFrom = (Double) from.get(1);
                        Double latTo = (Double) to.get(0);
                        Double lngTo = (Double) to.get(1);

                        Double distance = calDistance(latFrom, lngFrom, latTo, lngTo);

                        Edge newEdge = new Edge(
                                distance,
                                fromId,
                                toId,
                                key);
                        er.save(newEdge);
                    }
                }

                String[] navModes = { "Pedestrian", "ADA", "Vehicular" };

                for (String mode : navModes) {
                    NavMode curMode = new NavMode(null, mode);
                    navr.save(curMode);
                }

                log.info("Ran - Preloading data from phaseOne.json");

            } catch (Exception e) {
                log.error("Failed to load phaseOne.json", e);
            }
        };
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

}
