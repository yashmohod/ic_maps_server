package com.ops.ICmaps.Rotute;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "route")
public class Route {

    @Id
    private Long id;
    private String email;
    private boolean isAdmin;
    private boolean isRouteManager;

    protected Route() {

    }

    public Route(Long id, String email) {
        this.id = id;
        this.email = email;
        this.isAdmin = false;
        this.isRouteManager = false;

    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }

    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public Boolean isAdmin() {
        return this.isAdmin;
    }

    public void setIsRouteManager(boolean isRouteManager) {
        this.isRouteManager = isRouteManager;
    }

    public Boolean isRouteManager() {
        return this.isRouteManager;
    }
}