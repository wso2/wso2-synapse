package com.synapse.core.artifacts.mediators;

import com.synapse.core.synctx.MsgContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogMediatorTest {

    @Test
    void testExecute() {
        LogMediator logMediator = new LogMediator("INFO", "Test message", null);
        MsgContext context = new MsgContext();

        boolean result = logMediator.execute(context);

        assertTrue(result);
    }
}