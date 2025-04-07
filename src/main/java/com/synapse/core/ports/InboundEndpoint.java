package com.synapse.core.ports;

public interface InboundEndpoint {
    void start(InboundMessageMediator mediator) throws Exception;
    void stop() throws Exception;
}
