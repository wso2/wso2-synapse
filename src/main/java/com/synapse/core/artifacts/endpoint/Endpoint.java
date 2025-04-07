package com.synapse.core.artifacts.endpoint;

import com.synapse.core.artifacts.utils.Position;

public class Endpoint {
    private String name;
    private EndpointUrl endpointUrl;
    private String fileName;
    private Position position;

    public Endpoint() {
    }

    public Endpoint(String name, EndpointUrl endpointUrl, String fileName, Position position) {
        this.name = name;
        this.endpointUrl = endpointUrl;
        this.fileName = fileName;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EndpointUrl getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(EndpointUrl endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "Endpoint{" +
                "name='" + name + '\'' +
                ", endpointUrl=" + endpointUrl +
                ", fileName='" + fileName + '\'' +
                ", position=" + position +
                '}';
    }
}
