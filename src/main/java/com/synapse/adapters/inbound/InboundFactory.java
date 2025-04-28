package com.synapse.adapters.inbound;

import com.synapse.core.domain.InboundConfig;
import com.synapse.core.ports.InboundEndpoint;

public class InboundFactory {

    public static final String ERR_INBOUND_TYPE_NOT_FOUND = "Inbound type not found";

    public static InboundEndpoint newInbound(InboundConfig config) throws Exception {
        switch (config.getProtocol()) {
            case "file":
                return new FileInboundEndpoint(config);
            case "http":
                return new HttpInboundEndpoint(config);
            default:
                throw new Exception(ERR_INBOUND_TYPE_NOT_FOUND);
        }
    }
}
