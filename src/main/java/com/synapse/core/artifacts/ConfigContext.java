package com.synapse.core.artifacts;

import com.synapse.core.artifacts.api.API;
import com.synapse.core.artifacts.endpoint.Endpoint;
import com.synapse.core.artifacts.inbound.Inbound;

import java.util.HashMap;
import java.util.Map;

public class ConfigContext implements EndpointProvider{

    private final Map<String, API> apiMap;
    private final Map<String, Endpoint> endpointMap;
    private final Map<String, Sequence> sequenceMap;
    private final Map<String, Inbound> inboundMap;

    private ConfigContext() {
        this.apiMap = new HashMap<>();
        this.endpointMap = new HashMap<>();
        this.sequenceMap = new HashMap<>();
        this.inboundMap = new HashMap<>();
    }

    // Singleton instance
    private static final ConfigContext INSTANCE = new ConfigContext();

    public static ConfigContext getInstance() {
        return INSTANCE;
    }

    public void addAPI(API api) {
        apiMap.put(api.getName(), api);
    }

    public void addEndpoint(Endpoint endpoint) {
        endpointMap.put(endpoint.getName(), endpoint);
    }

    public void addSequence(Sequence sequence) {
        sequenceMap.put(sequence.getName(), sequence);
    }

    public void addInbound(Inbound inbound) {
        inboundMap.put(inbound.getName(), inbound);
    }

    @Override
    public Endpoint getEndpoint(String epName) {
        return endpointMap.getOrDefault(epName, new Endpoint());
    }

    public Map<String, API> getApiMap() {
        return apiMap;
    }

    public Map<String, Endpoint> getEndpointMap() {
        return endpointMap;
    }

    public Map<String, Sequence> getSequenceMap() {
        return sequenceMap;
    }

    public Map<String, Inbound> getInboundMap() {
        return inboundMap;
    }
}
