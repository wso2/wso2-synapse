package org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers.event;

import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;

public interface CloseEventHandler {
    void handleCloseEntryEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx);

    void handleCloseFlowForcefully(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx);

    void handleTryEndFlow(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx);
}
