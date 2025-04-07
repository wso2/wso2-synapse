package com.synapse.core.artifacts.inbound;

import com.synapse.core.artifacts.utils.Position;
import java.util.List;

public class Inbound {
    private String name;
    private String sequence;
    private String protocol;
    private String suspend;
    private String onError;
    private List<Parameter> parameters;
    private Position position;

    public Inbound() {
    }

    public Inbound(String name, String sequence, String protocol, String suspend, String onError, List<Parameter> parameters, Position position) {
        this.name = name;
        this.sequence = sequence;
        this.protocol = protocol;
        this.suspend = suspend;
        this.onError = onError;
        this.parameters = parameters;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getSuspend() {
        return suspend;
    }

    public void setSuspend(String suspend) {
        this.suspend = suspend;
    }

    public String getOnError() {
        return onError;
    }

    public void setOnError(String onError) {
        this.onError = onError;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "Inbound{" +
                "name='" + name + '\'' +
                ", sequence='" + sequence + '\'' +
                ", protocol='" + protocol + '\'' +
                ", suspend='" + suspend + '\'' +
                ", onError='" + onError + '\'' +
                ", parameters=" + parameters +
                ", position=" + position +
                '}';
    }
}