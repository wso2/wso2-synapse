package com.synapse.core.artifacts;

import com.synapse.core.artifacts.api.API;
import com.synapse.core.artifacts.inbound.Inbound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigContextTest {

    @Test
    void testAddAndGetAPI() {
        ConfigContext context = ConfigContext.getInstance();
        API api = new API();
        api.setName("TestAPI");

        context.addAPI(api);

        assertEquals(api, context.getApiMap().get("TestAPI"));
    }

    @Test
    void testAddAndGetInbound() {
        ConfigContext context = ConfigContext.getInstance();
        Inbound inbound = new Inbound();
        inbound.setName("TestInbound");

        context.addInbound(inbound);

        assertEquals(inbound, context.getInboundMap().get("TestInbound"));
    }
}