package com.synapse.adapters.inbound;

import com.synapse.core.domain.InboundConfig;
import org.apache.commons.vfs2.FileSystemException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileInboundEndpointTest {

    @Test
    void testGetIntervalParameterValue() throws FileSystemException {
        InboundConfig config = new InboundConfig(
                "fileInbound",
                "file",
                Map.of("interval", "5000"),
                "inboundSeq",
                "faultSeq"
        );

        FileInboundEndpoint endpoint = new FileInboundEndpoint(config);
        int interval = endpoint.getIntervalParameterValue();

        assertEquals(5000, interval);
    }

    @Test
    void testGetIntervalParameterValueInvalid() throws FileSystemException {
        InboundConfig config = new InboundConfig(
                "fileInbound",
                "file",
                Map.of("interval", "invalid"),
                "inboundSeq",
                "faultSeq"
        );

        FileInboundEndpoint endpoint = new FileInboundEndpoint(config);
        int interval = endpoint.getIntervalParameterValue();

        assertEquals(0, interval);
    }
}