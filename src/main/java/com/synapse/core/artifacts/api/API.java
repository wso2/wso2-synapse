package com.synapse.core.artifacts.api;

import com.synapse.core.artifacts.utils.Position;

import java.util.List;

public class API {
    private String context;
    private String name;
    private List<Resource> resources;
    private Position position;

    public API(String context, String name, List<Resource> resources, Position position) {
        this.context = context;
        this.name = name;
        this.resources = resources;
        this.position = position;
    }

    public API() {

    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }
}