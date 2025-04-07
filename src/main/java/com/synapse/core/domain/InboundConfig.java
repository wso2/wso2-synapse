package com.synapse.core.domain;

import java.util.Map;

public class InboundConfig {
    private String name;
    private String protocol;
    private Map<String, String> parameters;
    private String sequenceName;
    private String faultSequenceName;

    public InboundConfig(String name, String protocol, Map<String, String> parameters,
                         String sequenceName, String faultSequenceName) {
        this.name = name;
        this.protocol = protocol;
        this.parameters = parameters;
        this.sequenceName = sequenceName;
        this.faultSequenceName = faultSequenceName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getSequenceName() {
        return sequenceName;
    }

    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public String getFaultSequenceName() {
        return faultSequenceName;
    }

    public void setFaultSequenceName(String faultSequenceName) {
        this.faultSequenceName = faultSequenceName;
    }
}