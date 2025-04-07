package com.synapse.core.artifacts;

import com.synapse.core.artifacts.endpoint.Endpoint;

public interface EndpointProvider {
    Endpoint getEndpoint(String epName);
}
