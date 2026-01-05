
package com.ops.ICmaps.Rotute;

import org.springframework.stereotype.Service;

@Service
public class RouteService {

    private RouteRepository ur;

    public RouteService(RouteRepository ur) {
        this.ur = ur;
    }
}