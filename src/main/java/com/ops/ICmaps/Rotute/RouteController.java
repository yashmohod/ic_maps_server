package com.ops.ICmaps.Rotute;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@CrossOrigin
@RequestMapping("/route")
public class RouteController {

    private final RouteRepository ur;
    private final ObjectMapper objectMapper;

    public RouteController(RouteRepository ur, ObjectMapper objectMapper) {
        this.ur = ur;
        this.objectMapper = objectMapper;
    }


}
